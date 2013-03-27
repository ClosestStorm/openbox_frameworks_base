/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include <android/log.h>
#define LOG_TAG "SmbJni"

#include "JNIHelp.h"
#include <jni.h>
#include "Scoped.h"
#include "libsmbclient.h"

#include <dirent.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <sys/types.h>
#include <sys/mount.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <linux/in6.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include <utime.h>
#include <utils/Log.h>

#include <binder/IBinder.h>
#include <binder/IServiceManager.h>
#include <binder/Parcel.h>
#include <utils/String8.h>
#include <ISambaService.h>

#define SMB_PACKAGE ("com/softwinner/netshare/SmbFile")
#define SMB_Callback ("com/softwinner/netshare/OnReceiverListenner")
#define MAX_PATH 1024 
#define MAX_UNC_LEN 1024

using namespace android;

struct list_dir{
	struct smbc_dirent *smbcdir;
	//struct in_addr ipaddr;
	struct list_dir *next;
};
static const char *classPathName = SMB_PACKAGE;
static const char *username = NULL, *password = NULL, *workgroup = NULL;
sp<ISambaService> sSmbService;
const sp<ISambaService>& getSambaService(){ 
	if(0 == sSmbService.get()){
		sp<IServiceManager> sm = defaultServiceManager();
		sp<IBinder> am;
		do{
			am = sm->getService(String16("netshared.smb"));
			if(am != 0){
				break;
			}
			usleep(500000);
		}while(true);
		sSmbService = interface_cast<ISambaService>(am);
	}

	return sSmbService;
}

void deSambaService(){
	if(sSmbService.get() != NULL)
		sSmbService.clear();
}

static void get_auth_data(const char *srv,
							const char *shr,
							char *wg, int wglen,
							char *un, int unlen,
							char *pw, int pwlen){
	if(!username){
		strncpy(un, "guest", unlen-1);
	}else{
		strncpy(un, username, unlen-1);
	}

	if(!password){
		strncpy(un, "", unlen-1);
	}else{
		strncpy(pw, password, pwlen-1);
	}

	if(workgroup) strncpy(wg, workgroup, wglen-1);
}

static void Smb_initAuthentication(JNIEnv* env, jclass, jstring	domain,
								jstring uname, jstring passwd){

	if(!domain){
		workgroup = NULL;
	}else{
		workgroup = env->GetStringUTFChars(domain, NULL);
	}

	if(!uname){
		username = NULL;
	}else{
		username = env->GetStringUTFChars(uname,NULL);
	}

	if(!passwd){
		password = NULL;
	}else{
		password = env->GetStringUTFChars(passwd, NULL);
	}
}

static jboolean Smb_deleteImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (smbc_unlink(path.c_str()) == 0);
}

static bool doStat(JNIEnv* env, jstring javaPath, struct stat& sb) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (smbc_stat(path.c_str(), &sb) == 0);
}

static jlong Smb_lengthImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    if (!doStat(env, javaPath, sb)) {
        // We must return 0 for files that don't exist.
        // TODO: shouldn't we throw an IOException for ELOOP or EACCES?
        return 0;
    }

    /*
     * This android-changed code explicitly treats non-regular files (e.g.,
     * sockets and block-special devices) as having size zero. Some synthetic
     * "regular" files may report an arbitrary non-zero size, but
     * in these cases they generally report a block count of zero.
     * So, use a zero block count to trump any other concept of
     * size.
     *
     * TODO: why do we do this?
     */
    if (!S_ISREG(sb.st_mode) || sb.st_blocks == 0) {
        return 0;
    }
    return sb.st_size;
}

static jlong Smb_lastModifiedImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    if (!doStat(env, javaPath, sb)) {
        return 0;
    }
    return static_cast<jlong>(sb.st_mtime) * 1000L;
}

static jboolean Smb_isDirectoryImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    return (doStat(env, javaPath, sb) && S_ISDIR(sb.st_mode));
}

static jboolean Smb_isFileImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct stat sb;
    return (doStat(env, javaPath, sb) && S_ISREG(sb.st_mode));
}
static jboolean doAccess(JNIEnv* env, jstring javaPath, int mode) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }
    return (access(path.c_str(), mode) == 0);
}

static jboolean Smb_existsImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, F_OK);
}

static jboolean Smb_canExecuteImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, X_OK);
}

static jboolean Smb_canReadImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, R_OK);
}

static jboolean Smb_canWriteImpl(JNIEnv* env, jclass, jstring javaPath) {
    return doAccess(env, javaPath, W_OK);
}

static jstring Smb_readlink(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) 
        return NULL;

    // We can't know how big a buffer readlink(2) will need, so we need to
    // loop until it says "that fit".
    size_t bufSize = 512;
    while (true) {
        LocalArray<512> buf(bufSize);
        ssize_t len = readlink(path.c_str(), &buf[0], buf.size() - 1);
        if (len == -1) {
            // An error occurred.
            return javaPath;
        }
        if (static_cast<size_t>(len) < buf.size() - 1) {
            // The buffer was big enough.
            buf[len] = '\0'; // readlink(2) doesn't NUL-terminate.
            return env->NewStringUTF(&buf[0]);
        }
        // Try again with a bigger buffer.
        bufSize *= 2;
    }
}

static jboolean Smb_setLastModifiedImpl(JNIEnv* env, jclass, jstring javaPath, jlong ms) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    // We want to preserve the access time.
    struct stat sb;
    if (smbc_stat(path.c_str(), &sb) == -1) {
        return JNI_FALSE;
    }

    // TODO: we could get microsecond resolution with utimes(3), "legacy" though it is.
    utimbuf times;
    times.actime = sb.st_atime;
    times.modtime = static_cast<time_t>(ms / 1000);
    return (utime(path.c_str(), &times) == 0);
}

static jboolean doChmod(JNIEnv* env, jstring javaPath, mode_t mask, bool set) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    struct stat sb;
    if (smbc_stat(path.c_str(), &sb) == -1) {
        return JNI_FALSE;
    }
    mode_t newMode = set ? (sb.st_mode | mask) : (sb.st_mode & ~mask);
    //return (smbc_chmod(path.c_str(), newMode) == 0);
	return 1;
}

static jboolean Smb_setExecutableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IXUSR : (S_IXUSR | S_IXGRP | S_IXOTH), set);
}

static jboolean Smb_setReadableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IRUSR : (S_IRUSR | S_IRGRP | S_IROTH), set);
}

static jboolean Smb_setWritableImpl(JNIEnv* env, jclass, jstring javaPath,
        jboolean set, jboolean ownerOnly) {
    return doChmod(env, javaPath, ownerOnly ? S_IWUSR : (S_IWUSR | S_IWGRP | S_IWOTH), set);
}

static bool doStatFs(JNIEnv* env, jstring javaPath, struct statfs& sb) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    int rc = statfs(path.c_str(), &sb);
    return (rc != -1);
}

static jlong Smb_getFreeSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    return sb.f_bfree * sb.f_bsize; // free block count * block size in bytes.
}

static jlong Smb_getTotalSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    return sb.f_blocks * sb.f_bsize; // total block count * block size in bytes.
}

static jlong Smb_getUsableSpaceImpl(JNIEnv* env, jclass, jstring javaPath) {
    struct statfs sb;
    if (!doStatFs(env, javaPath, sb)) {
        return 0;
    }
    return sb.f_bavail * sb.f_bsize; // non-root free block count * block size in bytes.
}


// DirEntry and DirEntries is a minimal equivalent of std::forward_list
// for the filenames.
struct DirEntry {
    DirEntry(const char* filename) : name(strlen(filename)) {
        strcpy(&name[0], filename);
        next = NULL;
    }
    // On Linux, the ext family all limit the length of a directory entry to
    // less than 256 characters.
    LocalArray<256> name;
    DirEntry* next;
};

// Reads the directory referred to by 'pathBytes', adding each directory entry
// to 'entries'.
static bool readDirectory(JNIEnv* env, jstring javaPath, struct list_dir **entries,
								unsigned int *return_count, int *returnerr) {
    ScopedUtfChars path(env, javaPath);
	int fd;
	struct smbc_dirent* dirent;
    if (path.c_str() == NULL) {
		LOGD("Path is NULL!!!");
        return false;
    }

	if((fd = smbc_opendir(path.c_str())) < 0){
		LOGE("smbc_opendir is false");
		*returnerr = errno;
		return false;
	}

	while((dirent = smbc_readdir(fd)) != NULL){
		struct list_dir *tmp_list = (struct list_dir*)malloc(sizeof(struct list_dir));
		in_addr *iptmp;
		int dirsize = sizeof(struct smbc_dirent)
						+ dirent->namelen + dirent->commentlen + 1;
		if(tmp_list){
			tmp_list->next = (*entries);
			tmp_list->smbcdir = (struct smbc_dirent*)malloc(dirsize);
			if(!tmp_list->smbcdir){
				free(tmp_list);
				return true;
			}
			memset(tmp_list->smbcdir, '\0', dirsize);
			memcpy(tmp_list->smbcdir, dirent, dirsize);
			tmp_list->smbcdir->comment = (char *)(&tmp_list->smbcdir->name + dirent->namelen + 1);
			/*
			tmp_list->smbcdir->smbc_type = dirent->smbc_type;
			tmp_list->smbcdir->namelen = dirent->namelen;
			tmp_list->smbcdir->dirlen = dirent->dirlen;
			tmp_list->smbcdir->commentlen = dirent->commentlen;
			strncpy(tmp_list->smbcdir->name, (dirent->name), dirent->namelen + 1);
			tmp_list->smbcdir->comment = (char *)(&tmp_list->smbcdir->name + dirent->namelen + 1);
			strncpy(tmp_list->smbcdir->comment, (dirent->comment), dirent->commentlen + 1);
			*/
			(*entries) = tmp_list;
			(*return_count)++;
		}
	}

	if(fd > 1) smbc_closedir(fd);

    return true;
}

jobject ObjectSmbFile(JNIEnv* env, jstring javaPath,
						struct smbc_dirent *dir, struct in_addr *ipadr){
	jclass smbClass;
	jobject tmpObject;
	jfieldID jtype;
	jfieldID jdirlen;
	jfieldID jcommentlen;
	jfieldID jcomment;
	jfieldID jnamelen;
	jfieldID jname;
	jmethodID jmid;

	char initpath[MAX_PATH] = {0};
	int opt = 0;
    ScopedUtfChars path(env, javaPath);

	if( dir && (dir->smbc_type == SMBC_SERVER) ){
		strncpy(initpath, "smb://", 6);
		opt += 6;
	}else{
		strncpy(initpath, path.c_str(), path.size());
		opt += path.size();
	}
	if((opt+dir->namelen + 1) <= MAX_PATH){
		if(opt > 6){
			strncat(initpath, "/", 1);
		}
		strncat(initpath, dir->name, dir->namelen);
	}

	smbClass = env->FindClass(SMB_PACKAGE);
	if (smbClass == NULL){
        LOGD("Could not find class '%s'", SMB_PACKAGE);
		return NULL;
	}

	jmid = env->GetMethodID(smbClass, "<init>", "(Ljava/lang/String;)V");
	if (jmid == NULL){
        LOGD("Could not find class '%s'", SMB_PACKAGE);
		return NULL;
	}

	jtype = env->GetFieldID(smbClass,"type", "I");
	if (jtype == NULL){
        LOGD("Could not find jfieldID '%s'", "type");
		return NULL;
	}

	jdirlen = env->GetFieldID(smbClass,"dirlen", "I");
	if (jdirlen == NULL){
        LOGD("Could not find jfieldID '%s'", "dirlen");
		return NULL;
	}

	jcommentlen = env->GetFieldID(smbClass,"commentlen", "I");
	if (jcommentlen == NULL){
        LOGD("Could not find jfieldID '%s'", "commentlen");
		return NULL;
	}

	jcomment = env->GetFieldID(smbClass, "comment", "Ljava/lang/String;");
	if (jcomment == NULL){
        LOGD("Could not find jfieldID '%s'", "comment");
		return NULL;
	}

	jnamelen = env->GetFieldID(smbClass, "namelen", "I");
	if(jnamelen == NULL){
        LOGD("Could not find jfieldID '%s'", "namelen");
		return NULL;
	}

	jname = env->GetFieldID(smbClass, "name", "Ljava/lang/String;");
	if(jname == NULL){
        LOGD("Could not find jfieldID '%s'", "name");
		return NULL;
	}

	tmpObject = env->NewObject(smbClass, jmid, env->NewStringUTF(initpath));
	ScopedLocalRef<jstring> javaSmbComment(env, env->NewStringUTF(dir->comment));
	ScopedLocalRef<jstring> javaSmbName(env, env->NewStringUTF(dir->name));
	env->SetObjectField(tmpObject, jcomment, javaSmbComment.get());
	if (env->ExceptionCheck()) {
		goto objectsmbc_error;
	}

	env->SetObjectField(tmpObject, jname, javaSmbName.get());
	if (env->ExceptionCheck()) {
		goto objectsmbc_error;
	}

	env->SetIntField(tmpObject, jtype, dir->smbc_type);
	env->SetIntField(tmpObject, jdirlen, dir->dirlen);
	env->SetIntField(tmpObject, jcommentlen, dir->commentlen);
	env->SetIntField(tmpObject, jnamelen, dir->namelen);

	return tmpObject;

objectsmbc_error:
	if(tmpObject != NULL)
        env->DeleteLocalRef(tmpObject);
	LOGD("ObjectSmbFile is error!");
	return NULL;
}

/*
 * not callback, but it will return SmbFile[] for java.
 */
static jobjectArray Smb_listImpl(JNIEnv* env, jclass, jstring javaPath) {
    // Read the directory entries into an intermediate form.
    //DirEntries files;
	unsigned int i=0,count=0;
	struct list_dir *entrydir = NULL;
	struct list_dir *tmp = NULL;
	int geterr;
    if (!readDirectory(env, javaPath, &entrydir, &count, &geterr)) {
        return NULL;
    }

	tmp = entrydir;
	jobjectArray result = env->NewObjectArray(count, env->FindClass(SMB_PACKAGE), NULL);
	if (env->ExceptionCheck()) {
		goto error_return;
	}
	while(tmp && (count > 0)){
        env->SetObjectArrayElement(result, i,
							ObjectSmbFile(env, javaPath, tmp->smbcdir, NULL));
        if (env->ExceptionCheck()) {
            goto error_return;
        }
		entrydir = tmp->next;
		free(tmp->smbcdir);
		free(tmp);
		tmp = entrydir;
		i++;
	}

    // Translate the intermediate form into a Java String[].
    return result;

error_return:
	if(result != NULL)
		env->DeleteLocalRef(result);
	while(tmp){
		entrydir = tmp->next;
		free(tmp->smbcdir);
		free(tmp);
		tmp = entrydir;
	}
	LOGD("Could not get objectArray");
	return NULL;
}

/*
 * It will use JavaCallback. And it will return errno, on error.
 */
/*
 *On success, zero is returned. 
 *On error, -1 is returned errno is set appropriately.
 *
 * EACCES 13, "Permission denied".
 * EINVAL 22, "A NULL file/URL was passed, or the URL would not parse,".
 * ENOENT 2  "Durl does not exist, or name is an"
 * ENOTDIR 20 "Name is not a directory".
 * EPERM   1  "The workgroup could not be found".
 * ENODEV 19  "The workgroup or server could not be found".
 * ENOMEM 12  "Insufficient memory to complete the operation". 
 */
static int Smb_listCallbackImpl(JNIEnv* env, jclass, jstring javaPath, jobject obj) {
    // Read the directory entries into Callback function.
	
	jmethodID jcallback;
	jclass jbackobj;
	struct smbc_dirent* dirent;
	int fd;

    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return EINVAL;
    }

	if(!obj)
		return 0;
	jbackobj = env->GetObjectClass(obj);
	jcallback = env->GetMethodID(jbackobj, "onReceiver",
						"(Lcom/softwinner/netshare/SmbFile;)V");
	if(jcallback == NULL){
		LOGD("Could not find the \"onReceiver\"");
		return -1;
	}

	if((fd = smbc_opendir(path.c_str())) < 0){
		LOGE("smbc_opendir is false");
		//*returnerr = errno;
		return errno;
	}

	while((dirent = smbc_readdir(fd)) != NULL){
		env->CallVoidMethod(obj, jcallback, ObjectSmbFile(env, javaPath, dirent, NULL));
	}

	if(fd > 1) smbc_closedir(fd);

    return 0;
}


static jboolean Smb_mkdirImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

    // On Android, we don't want default permissions to allow global access.
    return (smbc_mkdir(path.c_str(), S_IRWXU) == 0);
}

static jboolean Smb_createNewFileImpl(JNIEnv* env, jclass, jstring javaPath) {
    ScopedUtfChars path(env, javaPath);
    if (path.c_str() == NULL) {
        return JNI_FALSE;
    }

	int fd = smbc_creat(path.c_str(), 0600);

    if (fd > 0) {
        // We created a new file. Success!
        return JNI_TRUE;
    }
    if (fd == EEXIST) {
        // The file already exists.
        return JNI_FALSE;
    }
    jniThrowIOException(env, fd);
    return JNI_FALSE; // Ignored by Java; keeps the C++ compiler happy.
}

static jboolean Smb_renameToImpl(JNIEnv* env, jclass, jstring javaOldPath, jstring javaNewPath) {
    ScopedUtfChars oldPath(env, javaOldPath);
    if (oldPath.c_str() == NULL) {
        return JNI_FALSE;
    }

    ScopedUtfChars newPath(env, javaNewPath);
    if (newPath.c_str() == NULL) {
        return JNI_FALSE;
    }

    return (rename(oldPath.c_str(), newPath.c_str()) == 0);
}

static jint Smb_getTypeImpl(JNIEnv* env, jclass, jstring javaPath){
    ScopedUtfChars path(env, javaPath);
	int fd;
	struct smbc_dirent* dirent;

	if((fd = smbc_opendir(path.c_str())) < 0){
		return -1;
	}
	dirent = smbc_readdir(fd);
	if(fd > 0){
		smbc_closedir(fd);
	}
	return dirent != NULL ? dirent->smbc_type : 0;
}

int is_ipaddress(const char *str)
{
  int pure_address = 1;
  int i;
  
  for (i=0; pure_address && str[i]; i++)
    if (!(isdigit((int)str[i]) || str[i] == '.'))
      pure_address = 0;

  /* Check that a pure number is not misinterpreted as an IP */
  pure_address = pure_address && (strchr(str, '.') != NULL);

  return pure_address;
}

/*
 *On success, zero is returned. 
 *On error, -1 is returned errno is set appropriately.
 *
 * EACCES 13, "Permission denied".
 * EBUSY  16, "Device or resource busy".
 * EFAULT 14, "One of the arguments points outside the user address space".
 * EINVAL 22, "Source had an invalid superblock".
 * ELOOP  40, "Too many links encountered during pathname resolution".
 * EMFILE 24, "In case no block device is required:) Table of dummy devices is full".
 * ENAMETOOLONG 36, "A pathname was longer than MAXPATHLEN".
 * ENODEV 19, "Filesystemtype not configured in the kernel".
 * ENOENT 2,  "A pathname was empty or had a nonexistent component".
 * ENOMEM 12, "Kernel couldn't  allocate a free page to copy filenames or data into".
 * ENOTBLK 15, "Source is not a block device (and a device was required)".
 * ENOTDIR 20, "Target, or a prefix of source, is not a directory".
 * ENXIO  6,  "The major number of the block device source is out of range".
 * EPERM  1,  "The caller does not have the required privileges".
 *
 */

static jint Smb_mount(JNIEnv* env, jclass, jstring source,
					jstring target, jstring username, jstring password){

	char *options, *share_name, *org_share, *tmp;
	char ipaddr[64] = {'\0'};
	int status = -2;
	int optlen;
	struct addrinfo *addrlist;
	struct sockaddr_in *addr4;
	struct sockaddr_in6 *addr6;
	struct hostent *ht = NULL;

    ScopedUtfChars sourcedir(env, source);
    ScopedUtfChars mountpoint(env, target);
    ScopedUtfChars user(env, username);
    ScopedUtfChars passwd(env, password);


	if(sourcedir.c_str() && mountpoint.c_str()){
		optlen = sourcedir.size() + 100;
		if(user.c_str())
			optlen += user.size();
		if(passwd.c_str())
			optlen += passwd.size(); 
		org_share = share_name = strndup(sourcedir.c_str(), MAX_UNC_LEN);
		if(!share_name)
			goto error_back;
		options = (char *)malloc(optlen);
		if(!options)
			goto error_back;
		memset(options, '\0', optlen);

		/*  It don't need word any more before '\\' or '//'!  */
		if((tmp = strstr(share_name, "//")) ||
			(tmp = strstr(share_name, "\\\\"))){
			share_name = tmp;
		}
	
		strlcpy(options, "unc=", optlen);
		strlcat(options, share_name, optlen);
		tmp = strrchr(options, '/');

		/*   yes, we must check 'unc=//'!  */
		if(tmp > options + 6)
			*tmp = '\\';
		strlcat(options, ",ver=1", optlen);
		if(user.c_str()){
			strlcat(options, ",user=", optlen);
			strlcat(options, user.c_str(), optlen);
		}
		if(passwd.c_str()){
			strlcat(options, ",pass=", optlen);
			strlcat(options, passwd.c_str(), optlen);
		}

		/*  OK, now we get the servername here!  */
		if(!strncmp(share_name, "//", 2) || !strncmp(share_name, "\\\\", 2)){
			share_name +=2;
			tmp = NULL;
			if(tmp = strpbrk(share_name, "/\\"))
				*tmp = 0;

			if(is_ipaddress(share_name)){
				strncpy(ipaddr, share_name, strlen(share_name));
			} else if(!getaddrinfo(share_name, NULL, NULL, &addrlist)){
				switch(addrlist->ai_addr->sa_family){
				case AF_INET6:
					addr6 = (struct sockaddr_in6*) addrlist->ai_addr;
					inet_ntop(AF_INET6, &addr6->sin6_addr, (char *)ipaddr, 64); 
					break;
				case AF_INET:
					addr4 = (struct sockaddr_in*) addrlist->ai_addr;
					inet_ntop(AF_INET, &addr4->sin_addr, (char *)ipaddr, 64); 
					break;
				}
			} else {
				struct in_addr *tmpip;
				tmpip = smbc_serverip(share_name);
				if(tmpip){
					strcpy(ipaddr, (char *)inet_ntoa(*tmpip));
				}
			}
			if(ipaddr[0] != (char)(0)){
				strlcat(options, ",ip=", optlen);
				strlcat(options, ipaddr, optlen);
			}

			/*
			ht = gethostbyname(share_name);
			if(ht){
				LOGD("gethostbyname: IP: %s\n", ht->h_addr_list[0]);
			}else{
				LOGD("gethostbyname is failed");
			}
			*/
			if(tmp)
				*tmp = '/';
			share_name -= 2;
		}
	}

	//LOGD("share_name: %s, mountpoint: %s, options: %s\n",
	//		share_name, mountpoint.c_str(), options);
	status = getSambaService()->vbser_mount(share_name, (char *)mountpoint.c_str(),
								"cifs", MS_MANDLOCK, options);
	deSambaService();

error_back:
	if(options)
		free(options);
	if(org_share)
		free(org_share);
	return status;
}

/*
 *On success, zero is returned. 
 *On error, -1 is returned errno is set appropriately.
 *
 * EAGAIN 11 
 * EBUSY  16, "Target could not be unmounted because it is busy".
 * EFAULT 14, "Target points outside the user address space".
 * EINVAL 22, "Ttarget is not a mount point". 
 * ENAMETOOLONG 36, "A pathname was longer than MAXPATHLEN".
 * ENOENT 2,  "A pathname was empty or had a nonexistent component".
 * ENOMEM 12, "Kernel could not allocate a free page to copy filenames or data into".
 * EPERM  1,  "The caller does not have the required privileges".
 *
 */
static jint Smb_umount(JNIEnv* env, jclass, jstring mountpoint){
    ScopedUtfChars path(env, mountpoint);
	int status = -2;
	
	if (path.c_str()){
		status = getSambaService()->vbser_umount((char *)path.c_str());
		deSambaService();
	}

	return status;
}


static JNINativeMethod Methods[] = {
    NATIVE_METHOD(Smb, canExecuteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, canReadImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, canWriteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, createNewFileImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, deleteImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, existsImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, getFreeSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(Smb, getTotalSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(Smb, getUsableSpaceImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(Smb, isDirectoryImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, isFileImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, lastModifiedImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(Smb, lengthImpl, "(Ljava/lang/String;)J"),
    NATIVE_METHOD(Smb, listImpl, "(Ljava/lang/String;)[Lcom/softwinner/netshare/SmbFile;"),
    NATIVE_METHOD(Smb, listCallbackImpl, "(Ljava/lang/String;Lcom/softwinner/netshare/OnReceiverListenner;)I"),
    NATIVE_METHOD(Smb, mkdirImpl, "(Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, readlink, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(Smb, renameToImpl, "(Ljava/lang/String;Ljava/lang/String;)Z"),
    NATIVE_METHOD(Smb, setExecutableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(Smb, setLastModifiedImpl, "(Ljava/lang/String;J)Z"),
    NATIVE_METHOD(Smb, setReadableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(Smb, setWritableImpl, "(Ljava/lang/String;ZZ)Z"),
    NATIVE_METHOD(Smb, getTypeImpl, "(Ljava/lang/String;)I"),
    NATIVE_METHOD(Smb, initAuthentication, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
    NATIVE_METHOD(Smb, mount, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I"),
    NATIVE_METHOD(Smb, umount, "(Ljava/lang/String;)I"),

};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                Methods, NELEM(Methods))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


/*
 * This is called by the VM when the shared library is first loaded.
 */
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }

	if(smbc_init(get_auth_data, 0)){
		LOGE("smbc_init FAILED!");
        goto bail;
	}
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
