//
// Created by Marvin on 10/31/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_OSCILLATOR_H
#define SYNTHESIZERAPP_NATIVE_OSCILLATOR_H

#include <cmath>
#include "native-oscillator-state.h"

class Oscillator {


public:
    Oscillator()
    {

        mLUTSize = 4096;
        mLUTSizeM = mLUTSize -1;
        mLUTSizeF = (float) mLUTSize;
        mLUT = new float[mLUTSize];

        for(int i = 0; i < mLUTSize ; i++)
        {
            mLUT[i] = cosf(i/mLUTSizeF*2.0f*(float)M_PI);
        }

        for(int i = 0; i < 4; i++)
        {
            mInputAmp[i] = 0.0f;
        }
    }

    ~Oscillator()
    {
        delete mLUT;
    }

    inline float lerp(float phase, float buffer [], int mask)
    {
        int intPart = (int) phase;
        float fracPart = phase-intPart;
        float a = buffer[intPart & mask];
        float b = buffer[(intPart+1) & mask];
        return a + (b - a) * fracPart;
    }

    inline float process(OscillatorState &oscillatorState)
    {

        if(oscillatorState.mPhase >= 1.0f)
        {
            oscillatorState.mPhase -= 1.0f;
        }else if(oscillatorState.mPhase < 0)
        {
            oscillatorState.mPhase += 1.0f;
        }

        float modulationTotal = 0.0f;
        for(int i = 0 ; i < 4; i ++)
        {
            modulationTotal += oscillatorState.mMod[i] * mInputAmp[i];
        }





        const float output = lerp((oscillatorState.mPhase + modulationTotal) * mLUTSizeF, mLUT, mLUTSizeM);


        //oscillatorState.mPrev = oscillatorState.mPhaseIncrement;


        oscillatorState.mPhase += oscillatorState.mPhi;



        return output;
    }

    float  mInputAmp[4];

private:
    float *mLUT;
    int mLUTSize;
    int mLUTSizeM;
    float mLUTSizeF;
};
#endif //SYNTHESIZERAPP_NATIVE_OSCILLATOR_H
