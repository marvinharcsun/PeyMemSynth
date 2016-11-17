//
// Created by Marvin on 10/31/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_ADSR_H
#define SYNTHESIZERAPP_NATIVE_ADSR_H


#include "native-adsr-state.h"
#include <cmath>
class ADSR {
public:
    ADSR()
    {
        setAttackLevel(1.0f);
        setAttackRate(0.01f);
        setAttackShape(1.0f);
        setDecayRate(0.01f);
        setDecayShape(1.0f);
        setSustainLevel(1.0f);
        setReleaseRate(0.01f);
        setReleaseShape(1.0f);
        setSampleRate(44100);
    }
    float process(ADSRState &adsrState)
    {
        switch (adsrState.mStage)
        {
            case kIdle:
                adsrState.mCounter = 0;
                break;
            case kStageAttack:
                adsrState.mEnvValue = powf(adsrState.mCounter*mAttack, mAttackShape)*mAttackLevel;
                if(adsrState.mEnvValue >= mAttackLevel)
                {
                    adsrState.mEnvValue = mAttackLevel;
                    adsrState.mStage = kStageDecay;
                    adsrState.mCounter = 0;
                }
                break;
            case kStageDecay:
                if(mAttackLevel >= mSustainLevel)
                {
                    adsrState.mEnvValue = mAttackLevel -  powf(adsrState.mCounter * mDecay, mDecayShape) * (mAttackLevel-mSustainLevel);
                    if(adsrState.mEnvValue <= mSustainLevel)
                    {
                        adsrState.mCounter = 0;
                        adsrState.mEnvValue = mSustainLevel;
                        adsrState.mStage = kStageSustain;
                    }
                }
                else if (mAttackLevel < mSustainLevel)
                {
                    adsrState.mEnvValue = mAttackLevel + powf(adsrState.mCounter * mDecay, mDecayShape) * (mSustainLevel-mAttackLevel);
                    if(adsrState.mEnvValue >= mSustainLevel)
                    {
                        adsrState.mCounter = 0;
                        adsrState.mEnvValue = mSustainLevel;
                        adsrState.mStage = kStageSustain;
                    }
                }
                break;
            case kStageSustain:
                adsrState.mCounter = 0;
                adsrState.mEnvValue = mSustainLevel;
                if(adsrState.mLoop)
                {
                    adsrState.mStage = kStageAttack;
                    adsrState.mCounter = 0;
                    adsrState.mEnvValue = 0.0f;
                }

                break;
            case kStageRelease:
                adsrState.mEnvValue  = mSustainLevel-powf(adsrState.mCounter*mRelease, mReleaseShape)*mSustainLevel;
                if (adsrState.mEnvValue  < 0.001f) {
                    adsrState.mCounter = 0;
                    adsrState.mEnvValue  = 0.0f;
                    adsrState.mStage = kIdle;
                }
                break;
            default:
                break;


        }

        adsrState.mCounter += 1.0f;
        return adsrState.mEnvValue * adsrState.mLevel;
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


    void setAttackLevel(float attackLevel)
    {
        if(attackLevel > 1.0f)
        {
            attackLevel = 1.0f;
        }
        else if(attackLevel <= .001f)
        {
            attackLevel = .001f;
        }

        mAttackLevel = attackLevel;

    }

    void setAttackShape(float attackShape)
    {
        if(attackShape <= .01f)
        {
            attackShape = .01f;
        }
        else if(attackShape >= 100.f)
        {
            attackShape = 100.f;
        }
        mAttackShape = attackShape;
    }

    void setDecayShape(float decayShape)
    {
        if(decayShape <= .01f)
        {
            decayShape = .01f;
        }
        else if(decayShape >= 100.f)
        {
            decayShape = 100.f;
        }
        mDecayShape = decayShape;
    }

    void setReleaseShape(float releaseShape)
    {
        if(releaseShape <= .01f)
        {
            releaseShape = .01f;
        }
        else if(releaseShape >= 100.f)
        {
            releaseShape = 100.f;
        }
        mReleaseShape = releaseShape;
    }

    void setSampleRate(float sampleRate)
    {
        mSampleRate = sampleRate;
    }
private:
    float mSustainLevel;
    float mAttackLevel;
    float mAttack;
    float mDecay;
    float mRelease;
    float mAttackShape;
    float mDecayShape;
    float mReleaseShape;
    float mSampleRate;
};
#endif //SYNTHESIZERAPP_NATIVE_ADSR_H
