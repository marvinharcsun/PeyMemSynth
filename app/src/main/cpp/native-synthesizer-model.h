//
// Created by Marvin on 10/31/2016.
//

#ifndef SYNTHESIZERAPP_NATIVE_SYNTHESIZER_MODEL_H
#define SYNTHESIZERAPP_NATIVE_SYNTHESIZER_MODEL_H


#include <queue>
#include <jni.h>

#include "native-adsr1.h"
#include "native-oscillator.h"
#include "native-voice.h"

#define VOICES 4
#define EVENTS_DONE 9999999
const float  MAX_VOLUME  = .25f*(1.f/VOICES);

struct MidiEvent{
    int message;
    int note;
    int velocity;
};
class SynthesizerModel{
public:

    SynthesizerModel()
    {

        mono = 0;

        xorVoice = 0;

        for(int i = 0; i < 128; i ++)
        {
            mNotes[i] = EVENTS_DONE;
        }

        mActiveVoices = 0;

        mMaster = 1.0f;
        setSampleRate(44100);
        for(int i = 0; i < 4 ; i++)
        {
            mSemi[i] = 1.0f;
            mFine[i] = 0.0f;
            mOutput[i] = 0.0f;
            mPan[i] = .5f;
            mOperator[i] = 0.0f;

            mOsc[i].mInputAmp[0] = 0.0f;
            mOsc[i].mInputAmp[1] = 0.0f;
            mOsc[i].mInputAmp[2] = 0.0f;
            mOsc[i].mInputAmp[3] = 0.0f;


            mADSR[i].setAttackRate(0.01f);
            mADSR[i].setDecayRate(0.01f);
            mADSR[i].setSustainLevel(1.0f);
            mADSR[i].setReleaseRate(0.01f);
            mADSR[i].setSampleRate(44100);
        }



    }

    ~SynthesizerModel()
    {

    }

    void processReplacing(float *outputs, int bufferSize) {
        int sampleFrames = bufferSize/2;
        int event =0 ,frame=0, frames = 0 , i = 0;
        float left = 0.0f, right = 0.0f;

        if(mActiveVoices > 0 || mNotes[event] < sampleFrames) {
            while (frame < sampleFrames) {

                frames = mNotes[event++];
                if (frames > sampleFrames)
                {
                    frames = sampleFrames;
                }
                frames -= frame;
                frame += frames;

                while (--frames >= 0) {

                    left = 0.0f;
                    right = 0.0f;

                    for(int i =0; i < 4; i++)
                    {
                        mOperator[i] = 0.0f;
                    }

                    if(mono)
                    {

                            if (mVS[xorVoice].active()) {

                                for (int j = 0; j < 4; j++) {



                                        if ((mVS[xorVoice].mOS[j].mPhi < mVS[xorVoice].mOS[j].mTarget)){

                                                mVS[xorVoice].mOS[j].mPhi += mVS[xorVoice].mOS[j].mTarget * port;
                                                if (mVS[xorVoice].mOS[j].mPhi >= mVS[xorVoice].mOS[j].mTarget)
                                                {
                                                    mVS[xorVoice].mOS[j].mPhi = mVS[xorVoice].mOS[j].mTarget;
                                                }
                                        }
                                        else if ((mVS[xorVoice].mOS[j].mPhi > mVS[xorVoice].mOS[j].mTarget))
                                        {
                                                mVS[xorVoice].mOS[j].mPhi -= mVS[xorVoice].mOS[j].mTarget * port;
                                                if (mVS[xorVoice].mOS[j].mPhi <= mVS[xorVoice].mOS[j].mTarget)
                                                {
                                                    mVS[xorVoice].mOS[j].mPhi = mVS[xorVoice].mOS[j].mTarget;
                                                }
                                        }else{
                                            mVS[xorVoice].mOS[j].mPhi = mVS[xorVoice].mOS[j].mTarget;
                                        }



                                    mVS[xorVoice].mOS[0].mMod[j] = mOperator[j] = mOsc[j].process(mVS[xorVoice].mOS[j]) * mADSR[j].process(mVS[xorVoice].mAS[j]);
                                    mVS[xorVoice].mOS[1].mMod[j] = mOperator[j];
                                    mVS[xorVoice].mOS[2].mMod[j] = mOperator[j];
                                    mVS[xorVoice].mOS[3].mMod[j] = mOperator[j];

                                    left  += (1.f - mPan[j]) * (mOperator[j] * mOutput[j]);
                                    right += (0.f + mPan[j]) * (mOperator[j] * mOutput[j]);
                                }
                            }
                    }
                    else
                    {
                        for (int v = 0; v < VOICES; v++)
                        {
                            if (mVS[v].active())
                            {
                                for (int j = 0; j < 4; j++)
                                {
                                    mVS[v].mOS[0].mMod[j] =
                                    mOperator[j] = mOsc[j].process(mVS[v].mOS[j]) * mADSR[j].process(mVS[v].mAS[j]);
                                    mVS[v].mOS[1].mMod[j] = mOperator[j];
                                    mVS[v].mOS[2].mMod[j] = mOperator[j];
                                    mVS[v].mOS[3].mMod[j] = mOperator[j];
                                    left += (1.f - mPan[j]) * (mOperator[j] * mOutput[j]);
                                    right += (0.f + mPan[j]) * (mOperator[j] * mOutput[j]);
                                }
                            }
                        }
                    }

                    outputs[i] =  MAX_VOLUME * mMaster * left; //Left output
                    outputs[i + 1] = MAX_VOLUME * mMaster * right ; //Right output

                    i += 2;
                }

                if (frame < sampleFrames) {
                    int note = mNotes[event++];
                    int vel = mNotes[event++];
                    noteOn(note, vel);
                }


                mActiveVoices = VOICES;

                for(int v=0; v<VOICES; v++)
                {
                    if (!mVS[v].active())
                    {
                        mActiveVoices--;
                    }else{
                        mVS[v].age++;
                    }
                }

            }
        }else{
            for (int j = 0; j < bufferSize; j+=2 ) {
                outputs[j] = 0;
                outputs[j+1] = 0;
            }
        }

        mNotes[0] = EVENTS_DONE;

    }

    void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
        for(int i = 0; i < 4 ; i ++)
        {
            mADSR[i].setSampleRate(sampleRate);
        }
        fs = 1.f/mSampleRate;
    }


    void processEvents(std::queue<MidiEvent> &events, int deltaFrames) {
        int npos = 0;
        while(!events.empty()) {
            switch (events.front().message) {
                case 0x80: //note off
                    mNotes[npos++] = deltaFrames; //delta
                    mNotes[npos++] = events.front().note; //note
                    mNotes[npos++] = 0;           //vel
                    break;

                case 0x90: //note on
                    mNotes[npos++] = deltaFrames; //delta
                    mNotes[npos++] = events.front().note; //note
                    mNotes[npos++] = events.front().velocity; //vel
                    break;

            }
            if(npos>128) npos -= 3;
            events.pop();
        }

        mNotes[npos] = EVENTS_DONE;

    }
    float mSemi[4];
    float mFine[4];
    float mPan[4];
    float mOutput[4];
    float mMaster;

    float fs;
    float port;
    int mono;
    int mSampleRate;

    ADSR mADSR[4];
    Oscillator mOsc[4];
    VoiceState mVS[VOICES];


private:


    int findFreeVoice() {

        int i = 0;

        while(i < VOICES) {
            if (!mVS[i].active()) {
                mVS[i].age = 0;
                return i;
            }else{
                mVS[i].age++;
            }
            i++;
        }


        int currentIndex=1;
        int oldestIndex=0;

        while (currentIndex<VOICES) {
            if (mVS[currentIndex].age > mVS[oldestIndex].age) {
                oldestIndex = currentIndex;
            }
            currentIndex++;

        }

        mVS[oldestIndex].age = 0;


        return oldestIndex;
    }


    void noteOn(int note, int velocity) {

        int v = 0;
        mActiveVoices = VOICES;

        if(velocity > 0)
        {

            if(mono)
            {
                v = xorVoice ^= 1;
            }else {
                v = findFreeVoice();
            }

            mVS[v].mKey = note;
            for(int i = 0; i < 4; i++) {
                mVS[v].mOS[i].mTarget = mVS[v].mOS[i].mPhi =(powf(2.0, ((note + mSemi[i] + (mFine[i])) - 69.0f) / 12.0f) * 440.0f) * fs;

                mVS[v].mOS[i].mPhase = 0.0f;
                mVS[v].mAS[i].mLevel = velocity / 127.f;
                mVS[v].mAS[i].mStage = kStageAttack;
                mVS[v].mAS[i].mEnvValue = 0.0f;

                if(mono && mVS[xorVoice^1].mKey > -1) {
                    mVS[xorVoice].mOS[i].mPhi = mVS[xorVoice ^ 1].mOS[i].mPhi;
                    mVS[v].mOS[i].mPhase = mVS[xorVoice ^ 1].mOS[i].mPhase;
                    mVS[v].mAS[i].mLevel = velocity / 127.f;
                    mVS[v].mAS[i].mStage = mVS[v^1].mAS[i].mStage;
                    mVS[v].mAS[i].mEnvValue = mVS[v^1].mAS[i].mEnvValue;
                }

            }

        }else{

            if(mono)
            {


                for(int index = 0 ; index < 2; index++) {
                    if (mVS[index].mKey == note) {
                        if (mVS[index].active()) {
                            for (int i = 0; i < 4; i++) {
                                lastValues[i] = mVS[index].mAS[i].mEnvValue;
                                lastStages[i] = mVS[index].mAS[i].mStage;
                                lastPhases[i] = mVS[index].mOS[i].mPhase;
                                mVS[index].mAS[i].mStage = kStageRelease;
                            }
                        }
                        mVS[index].mKey = -1;
                    }
                }

                if(mVS[xorVoice^1].mKey > -1) {
                    xorVoice ^= 1;
                    for (int k = 0; k < 4; k++) {
                        mVS[xorVoice].mAS[k].mStage = lastStages[k];
                        mVS[xorVoice].mAS[k].mEnvValue = lastValues[k];
                        mVS[xorVoice].mOS[k].mPhase = lastPhases[k];
                        mVS[xorVoice].mOS[k].mPhi = mVS[xorVoice ^ 1].mOS[k].mPhi;
                    }
                }


            }else {
                for (int index = 0; index < VOICES; index++) {

                    if(mVS[index].mKey == note)
                    {
                        if(mVS[index].active())
                        {
                            for(int i =0;i<4;i++)
                            {
                                mVS[index].mAS[i].mStage=kStageRelease;
                            }
                            mVS[index].mKey=-1;
                            mVS[index].age++;
                            return;
                        }
                    }
                }
            }

        }


    }


    float mOperator[4];
    float lastValues[4];
    float lastPhases[4];
    int mNotes[128];
    int mActiveVoices;
    ADSRStage lastStages[4];
    int xorVoice;
};








#endif //SYNTHESIZERAPP_NATIVE_SYNTHESIZER_MODEL_H
