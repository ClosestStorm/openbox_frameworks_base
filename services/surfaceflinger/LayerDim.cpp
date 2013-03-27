/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdlib.h>
#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/Log.h>

#include <ui/GraphicBuffer.h>

#include "LayerDim.h"
#include "SurfaceFlinger.h"
#include "DisplayHardware/DisplayHardware.h"

namespace android {
// ---------------------------------------------------------------------------

LayerDim::LayerDim(SurfaceFlinger* flinger, DisplayID display,
        const sp<Client>& client)
    : LayerBaseClient(flinger, display, client)
{
}

LayerDim::~LayerDim()
{
}

void LayerDim::onDraw(const Region& clip) const
{	
    const State& s(drawingState());
    Region::const_iterator it = clip.begin();
    Region::const_iterator const end = clip.end();
    if (s.alpha>0 && (it != end)) {
        const DisplayHardware& hw(graphicPlane(0).displayHardware());
        const GLfloat alpha = s.alpha/255.0f;
        const uint32_t fbHeight = hw.getHeight();
        glDisable(GL_TEXTURE_EXTERNAL_OES);
        glDisable(GL_TEXTURE_2D);

        if (s.alpha == 0xFF) {
            glDisable(GL_BLEND);
        } else {
            glEnable(GL_BLEND);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        }

        glColor4f(0, 0, 0, alpha);

        glVertexPointer(2, GL_FLOAT, 0, mVertices);

        while (it != end) {
            const Rect& r = *it++;
            const GLint sy = fbHeight - (r.top + r.height());

			if(hw.setDispProp(DISPLAY_CMD_GETDISPLAYMODE,0,0,0) == DISPLAY_MODE_SINGLE_VAR_GPU)
			{				
				const DisplayHardware& hw(graphicPlane(0).displayHardware());
				
				int app_width = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_WIDTH,0);
				int app_height = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_HEIGHT,0);
				int vp_w = hw.getWidth() * mDispWidth / app_width;
				int vp_h = hw.getHeight() * mDispHeight / app_height;
				int scissor_w = hw.getWidth();
				int scissor_h = hw.getHeight();
			
				if(mDispWidth > app_width)
				{
					scissor_w = hw.getWidth()*mDispWidth/app_width;
				}
				if(mDispHeight > app_height)
				{
					scissor_h = hw.getHeight()*mDispHeight/app_height;
				}
				glScissor(0,0,scissor_w,scissor_h);
				
				if(graphicPlane(0).getOrientation() == 0)
				{
					glViewport(0, hw.getHeight() - vp_h, vp_w, vp_h);
				}
				else if(graphicPlane(0).getOrientation() == 1)
				{
					if(hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_OUTPUT_TYPE,0) != DISPLAY_DEVICE_LCD)
					{
						glLoadIdentity();				 
						glTranslatef(hw.getWidth()+hw.getHeight()-app_height,hw.getHeight()-hw.getWidth(),0.0f);
						glRotatef(90,0.0f,0.0f,1.0f);
						app_width = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_HEIGHT,0);
						app_height = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_WIDTH,0);
						vp_w = hw.getWidth() * mDispWidth / app_width;
						vp_h = hw.getHeight() * mDispHeight / app_height;
					}
					glViewport(hw.getWidth() - vp_w, hw.getHeight() - vp_h, vp_w, vp_h);
				}
				else if(graphicPlane(0).getOrientation() == 2)
				{
					if(hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_OUTPUT_TYPE,0) != DISPLAY_DEVICE_LCD)
					{
						glLoadIdentity();
						glTranslatef(hw.getWidth()+hw.getWidth()-app_width,app_height,0.0f);
						glRotatef(180,0.0f,0.0f,1.0f);
					}
					glViewport(hw.getWidth() - vp_w, 0, vp_w, vp_h);
				}
				else if(graphicPlane(0).getOrientation() == 3)
				{
					if(hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_OUTPUT_TYPE,0) != DISPLAY_DEVICE_LCD)
					{
						glLoadIdentity();
						glTranslatef(0.0f,app_width,0.0f);
						glRotatef(270,0.0f,0.0f,1.0f);
						app_width = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_HEIGHT,0);
						app_height = hw.setDispProp(DISPLAY_CMD_GETDISPPARA,0,DISPLAY_APP_WIDTH,0);
						vp_w = hw.getWidth() * mDispWidth / app_width;
						vp_h = hw.getHeight() * mDispHeight / app_height;
					}
					glViewport(0, 0, vp_w, vp_h);
				}
				LOGV("app_width:%d,app_height:%d,mDispWidth:%d,mDispHeight:%d,vp_w:%d,vp_h:%d\n",app_width,app_height,mDispWidth,mDispHeight,vp_w,vp_h);
			}
			else
			{
				glScissor(r.left, sy, r.width(), r.height());
			}
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4); 
        }
        glDisable(GL_BLEND);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    }
}

// ---------------------------------------------------------------------------

}; // namespace android
