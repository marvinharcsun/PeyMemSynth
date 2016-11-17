//
// Created by Marvin on 11/3/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_ADSR1_H
#define SYNTHESIZERAPP_NATIVE_ADSR1_H
#include "native-adsr-state1.h"
#include <cmath>
class ADSR {
public:
    ADSR()
    {
        setAttackRate(0.01f);
        setDecayRate(0.01f);
        setSustainLevel(1.0f);
        setReleaseRate(0.01f);
        setSampleRate(44100);
    }
    float process(ADSRState &adsrState)
    {
        switch (adsrState.mStage)
        {
            case kIdle:
                break;
            case kStageAttack:
                adsrState.mEnvValue += mAttack;
                if(adsrState.mEnvValue > 1.0f)
                {
                    adsrState.mEnvValue = 1.0f;
                    adsrState.mStage = kStageDecay;
                }
                break;
            case kStageDecay:
                adsrState.mEnvValue -= mDecay;
                if(adsrState.mEnvValue < mSustainLevel)
                {
                    adsrState.mEnvValue = mSustainLevel;
                    adsrState.mStage = kStageSustain;
                }
                break;
            case kStageSustain:
                adsrState.mEnvValue = mSustainLevel;
                break;
            case kStageRelease:
                adsrState.mEnvValue  -= mRelease;
                if (adsrState.mEnvValue  < 0.000001f) {
                    adsrState.mEnvValue  = 0.0f;
                    adsrState.mStage = kIdle;
                }
                break;
            default:
                break;


        }
      ;
        return adsrState.mEnvValue *adsrState.mLevel;
    }

    void setAttackRate(float attackRate)
    {
        if(attackRate <= 0.001f)
        {
            attackRate = 0.001f;
        }
        mAttack = (1.0f/ (mSampleRate*attackRate));
    }

    void setDecayRate(float decayRate)
    {
        if(decayRate <= 0.001f)
        {
            decayRate = 0.001f;
        }

        mDecay = (1.0f / (mSampleRate * decayRate));

    }

    void setReleaseRate(float releaseRate)
    {

        if(releaseRate <= 0.001f)
        {
            releaseRate = 0.001f;
        }

        mRelease = (1.0f / (mSampleRate * releaseRate));

    }

    void setSustainLevel(float sustainLevel)
    {
        if(sustainLevel > 1.0f)
        {
            sustainLevel = 1.0f;
        }
        else if(sustainLevel <= .001f)
        {
            sustainLevel = .001f;
        }

        mSustainLevel = sustainLevel;

    }

    void setSampleRate(float sampleRate)
    {
        mSampleRate = sampleRate;
    }
private:
    float mSustainLevel;
    float mAttack;
    float mDecay;
    float mRelease;
    float mSampleRate;
};
#endif //SYNTHESIZERAPP_NATIVE_ADSR1_H
