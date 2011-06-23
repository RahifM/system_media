/*
 * Copyright (C) 2011 The Android Open Source Project
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


package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.NativeFrame;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;

import android.util.Log;

public class ToGrayFilter extends SimpleImageFilter {

    @GenerateFieldPort(name = "outputChannels")
    private int mOutChannels;
    @GenerateFieldPort(name = "invertSource", hasDefault = true)
    private boolean mInvertSource = false;

    private MutableFrameFormat mOutputFormat;

    private static final String mColorToGray4Shader =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler_0;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(tex_sampler_0, v_texcoord);\n" +
            "  float y = dot(color, vec4(0.299, 0.587, 0.114, 0));\n" +
            "  gl_FragColor = vec4(y, y, y, color.a);\n" +
            "}\n";

    public ToGrayFilter(String name) {
        super(name, null);
    }

    @Override
    protected Program getNativeProgram() {
        /*switch (mOutChannels) {
          case 1:
          mProgram = new NativeProgram("filterpack_imageproc", "color_to_gray1");
          break;
          case 3:
          mProgram = new NativeProgram("filterpack_imageproc", "color_to_gray3");
          break;
          case 4:
          mProgram = new NativeProgram("filterpack_imageproc", "color_to_gray4");
          break;
          default:
          throw new RuntimeException("Unsupported output channels: " + mOutChannels + "!");
          }
          mProgram.setHostValue("inputChannels", mInputChannels);
          break;*/
        throw new RuntimeException("Native toGray not implemented yet!");
    }

    @Override
    protected Program getShaderProgram() {
        int inputChannels = getInputFormat("image").getBytesPerSample();
        if (inputChannels != 4 || mOutChannels != 4) {
            throw new RuntimeException("Unsupported GL channels: " + inputChannels + "/" +
                                       mOutChannels +
                                       "(in/out)! Both input and output channels " +
                                       "must be 4!");
        }
        ShaderProgram program = new ShaderProgram(mColorToGray4Shader);
        if (mInvertSource)
            program.setSourceRect(0.0f, 1.0f, 1.0f, -1.0f);
        return program;
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        mOutputFormat = inputFormat.mutableCopy();
        mOutputFormat.setBytesPerSample(mOutChannels);
        mOutputFormat.setMetaValue(ImageFormat.COLORSPACE_KEY, ImageFormat.COLORSPACE_GRAY);
        return mOutputFormat;
    }

    @Override
    public void process(FilterContext env) {
        // Get input frame
        Frame input = pullInput("image");

        // Create output frame
        MutableFrameFormat outputFormat = input.getFormat().mutableCopy();
        outputFormat.setBytesPerSample(mOutChannels);
        Frame output = env.getFrameManager().newFrame(outputFormat);

        // Make sure we have a program
        updateProgramWithTarget(input.getFormat().getTarget());

        // Process
        mProgram.process(input, output);

        // Push output
        pushOutput("image", output);

        // Release pushed frame
        output.release();
    }

}
