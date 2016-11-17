//
// Created by Marvin on 10/31/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_ADSR_STATE_H
#define SYNTHESIZERAPP_NATIVE_ADSR_STATE_H

#include <cmath>

enum ADSRStage
{
    kIdle,
    kStageAttack,
    kStageDecay,
    kStageSustain,
    kStageRelease,
};

struct ADSRState {

    ADSRState()
    {
        mEnvValue = 0.f;
        mLevel = 0.f;
        mCounter = 0.0f;
        mStage = kIdle;
        mLoop = 0;
    }

    float mEnvValue;
    float mPrev;
    float mLevel;
    float mCounter;
    int mLoop;
    ADSRStage mStage;

};

#endif //SYNTHESIZERAPP_NATIVE_ADSR_STATE_H
