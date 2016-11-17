//
// Created by Marvin on 10/31/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_OSCILLATOR_STATE_H
#define SYNTHESIZERAPP_NATIVE_OSCILLATOR_STATE_H

struct OscillatorState {

    OscillatorState()
    {
        mPhase = 0.0f;
        mPhi = 0.0f;
        for(int i = 0; i < 4; i++)
        {
            mMod[i] = 0.f;
        }
    }

    float mMod[4];
    float mPhase;
    float mPhi;
    float mTarget;
};

#endif //SYNTHESIZERAPP_NATIVE_OSCILLATOR_STATE_H
