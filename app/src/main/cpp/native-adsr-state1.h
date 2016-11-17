//
// Created by Marvin on 11/3/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_ADSR_STATE1_H
#define SYNTHESIZERAPP_NATIVE_ADSR_STATE1_H
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
        mPrev = 0.0f;
        mStage = kIdle;
    }

    float mPrev;
    float mEnvValue;
    float mLevel;
    ADSRStage mStage;

};
#endif //SYNTHESIZERAPP_NATIVE_ADSR_STATE1_H
