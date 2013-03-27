#ifndef SCOPED_H_included
#define SCOPED_H_included

#include "JNIHelp.h"
#include <string.h>
#include <unistd.h>


#define NATIVE_METHOD(className, functionName, signature) \
    { #functionName, signature, reinterpret_cast<void*>(className ## _ ## functionName) }

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv* env, jstring s)
    : mEnv(env), mString(s)
    {
        if (s == NULL) {
            mUtfChars = NULL;
            //jniThrowNullPointerException(env, NULL);
        } else {
            mUtfChars = env->GetStringUTFChars(s, NULL);
        }
    }

    ~ScopedUtfChars() {
        if (mUtfChars) {
            mEnv->ReleaseStringUTFChars(mString, mUtfChars);
        }
    }

    const char* c_str() const {
        return mUtfChars;
    }

    size_t size() const {
        return strlen(mUtfChars);
    }

    // Element access.
    const char& operator[](size_t n) const {
        return mUtfChars[n];
    }

private:
    JNIEnv* mEnv;
    jstring mString;
    const char* mUtfChars;

    // Disallow copy and assignment.
    ScopedUtfChars(const ScopedUtfChars&);
    void operator=(const ScopedUtfChars&);
};

class ScopedFd {
public:
    explicit ScopedFd(int fd) : fd(fd) {
    }

    ~ScopedFd() {
        close(fd);
    }

    int get() const {
        return fd;
    }

private:
    int fd;

    // Disallow copy and assignment.
    ScopedFd(const ScopedFd&);
    void operator=(const ScopedFd&);
};

template<typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv* env, T localRef)
    : mEnv(env), mLocalRef(localRef)
    {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset() {
        if (mLocalRef != NULL) {
            mEnv->DeleteLocalRef(mLocalRef);
            mLocalRef = NULL;
        }
    }

    T get() const {
        return mLocalRef;
    }

private:
    JNIEnv* mEnv;
    T mLocalRef;

    // Disallow copy and assignment.
    ScopedLocalRef(const ScopedLocalRef&);
    void operator=(const ScopedLocalRef&);
};

template <size_t STACK_BYTE_COUNT>
class LocalArray {
public:
    /**
     * Allocates a new fixed-size array of the given size. If this size is
     * less than or equal to the template parameter STACK_BYTE_COUNT, an
     * internal on-stack buffer will be used. Otherwise a heap buffer will
     * be allocated.
     */
    LocalArray(size_t desiredByteCount) : mSize(desiredByteCount) {
        if (desiredByteCount > STACK_BYTE_COUNT) {
            mPtr = new char[mSize];
        } else {
            mPtr = &mOnStackBuffer[0];
        }
    }

    /**
     * Frees the heap-allocated buffer, if there was one.
     */
    ~LocalArray() {
        if (mPtr != &mOnStackBuffer[0]) {
            delete[] mPtr;
        }
    }

    // Capacity.
    size_t size() { return mSize; }
    bool empty() { return mSize == 0; }

    // Element access.
    char& operator[](size_t n) { return mPtr[n]; }
    const char& operator[](size_t n) const { return mPtr[n]; }

private:
    char mOnStackBuffer[STACK_BYTE_COUNT];
    char* mPtr;
    size_t mSize;

    // Disallow copy and assignment.
    LocalArray(const LocalArray&);
    void operator=(const LocalArray&);
};

#endif  // JNI_CONSTANTS_H_included
