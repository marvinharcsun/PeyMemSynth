

#ifndef SYNTHESIZERAPP_NATIVE_VOICE_H
#define SYNTHESIZERAPP_NATIVE_VOICE_H

#include "native-oscillator-state.h"
#include "native-adsr-state1.h"

struct VoiceState {

    VoiceState()
    {
        for(int i = 0 ; i < 4 ; i++)
        {
            mOS[i].mPhase = 0.0f;
            mOS[i].mPhi = 0.0f;
            mAS[i].mEnvValue = 0;
            mAS[i].mStage = kIdle;
            mAS[i].mLevel = 1.0f;
        }
        mKey = -1;
        age = 0;
    }

    bool active()
    {
        return (mAS[0].mStage != kIdle ||
                mAS[1].mStage != kIdle ||
                mAS[2].mStage != kIdle ||
                mAS[3].mStage != kIdle);
    }

    OscillatorState mOS[4];
    ADSRState  mAS[4];
    int mKey;
    int age;

};
#endif //SYNTHESIZERAPP_NATIVE_VOICE_H
