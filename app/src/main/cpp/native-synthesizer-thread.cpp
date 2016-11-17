//
// Created by Marvin on 10/31/2016.
//

#include "native-synthesizer-thread.h"
#include "native-synthesizer-model.h"

#define CONV16BIT 32768
#define CONVMYFLT (1./32768.)
static void* createThreadLock(void);
static int waitThreadLock(void *lock);
static void notifyThreadLock(void *lock);
static void destroyThreadLock(void *lock);
static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);
static SynthesizerModel *synthesizerModel;
// creates the OpenSL ES audio engine
static SLresult openSLCreateEngine(OPENSL_STREAM *p)
{
    SLresult result;
    // create engine
    result = slCreateEngine(&(p->engineObject), 0, NULL, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS) goto  engine_end;

    // realize the engine
    result = (*p->engineObject)->Realize(p->engineObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS) goto engine_end;

    // get the engine interface, which is needed in order to create other objects
    result = (*p->engineObject)->GetInterface(p->engineObject, SL_IID_ENGINE, &(p->engineEngine));
    if(result != SL_RESULT_SUCCESS) goto  engine_end;

    engine_end:
    return result;
}
// opens the OpenSL ES device for output
static SLresult openSLPlayOpen(OPENSL_STREAM *p)
{
    SLresult result;
    SLuint32 sr = p->sr;
    SLuint32  channels = p->outchannels;

    if(channels){
        // configure audio source
        SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};

        switch(sr){

            case 8000:
                sr = SL_SAMPLINGRATE_8;
                break;
            case 11025:
                sr = SL_SAMPLINGRATE_11_025;
                break;
            case 16000:
                sr = SL_SAMPLINGRATE_16;
                break;
            case 22050:
                sr = SL_SAMPLINGRATE_22_05;
                break;
            case 24000:
                sr = SL_SAMPLINGRATE_24;
                break;
            case 32000:
                sr = SL_SAMPLINGRATE_32;
                break;
            case 44100:
                sr = SL_SAMPLINGRATE_44_1;
                break;
            case 48000:
                sr = SL_SAMPLINGRATE_48;
                break;
            case 64000:
                sr = SL_SAMPLINGRATE_64;
                break;
            case 88200:
                sr = SL_SAMPLINGRATE_88_2;
                break;
            case 96000:
                sr = SL_SAMPLINGRATE_96;
                break;
            case 192000:
                sr = SL_SAMPLINGRATE_192;
                break;
            default:
                return -1;
        }

        const SLInterfaceID ids[] = {SL_IID_VOLUME};
        const SLboolean req[] = {SL_BOOLEAN_FALSE};
        result = (*p->engineEngine)->CreateOutputMix(p->engineEngine, &(p->outputMixObject), 1, ids, req);
        if(result != SL_RESULT_SUCCESS)
        {
            return result;
        }

        // realize the output mix
        result = (*p->outputMixObject)->Realize(p->outputMixObject, SL_BOOLEAN_FALSE);

        int speakers;
        if(channels > 1)
            speakers = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
        else speakers = SL_SPEAKER_FRONT_CENTER;
        SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM,channels, sr,
                                       SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                       (SLuint32)speakers, SL_BYTEORDER_LITTLEENDIAN};

        SLDataSource audioSrc = {&loc_bufq, &format_pcm};

        // configure audio sink
        SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, p->outputMixObject};
        SLDataSink audioSnk = {&loc_outmix, NULL};

        // create audio player
        const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
        const SLboolean req1[] = {SL_BOOLEAN_TRUE};
        result = (*p->engineEngine)->CreateAudioPlayer(p->engineEngine, &(p->bqPlayerObject), &audioSrc, &audioSnk,
                                                       1, ids1, req1);
        if(result != SL_RESULT_SUCCESS) goto end_openaudio;

        // realize the player
        result = (*p->bqPlayerObject)->Realize(p->bqPlayerObject, SL_BOOLEAN_FALSE);
        if(result != SL_RESULT_SUCCESS) goto end_openaudio;

        // get the play interface
        result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_PLAY, &(p->bqPlayerPlay));
        if(result != SL_RESULT_SUCCESS) goto end_openaudio;

        // get the buffer queue interface
        result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                                    &(p->bqPlayerBufferQueue));
        if(result != SL_RESULT_SUCCESS) goto end_openaudio;

        // register callback on the buffer queue
        result = (*p->bqPlayerBufferQueue)->RegisterCallback(p->bqPlayerBufferQueue, bqPlayerCallback, p);
        if(result != SL_RESULT_SUCCESS) goto end_openaudio;

        // set the player's state to playing
        result = (*p->bqPlayerPlay)->SetPlayState(p->bqPlayerPlay, SL_PLAYSTATE_PLAYING);

        end_openaudio:
            return result;
    }
    return SL_RESULT_SUCCESS;
}
// close the OpenSL IO and destroy the audio engine
static void openSLDestroyEngine(OPENSL_STREAM *p){

    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (p->bqPlayerObject != NULL) {
        (*p->bqPlayerObject)->Destroy(p->bqPlayerObject);
        p->bqPlayerObject = NULL;
        p->bqPlayerPlay = NULL;
        p->bqPlayerBufferQueue = NULL;
        p->bqPlayerEffectSend = NULL;
    }

    // destroy audio recorder object, and invalidate all associated interfaces
    if (p->recorderObject != NULL) {
        (*p->recorderObject)->Destroy(p->recorderObject);
        p->recorderObject = NULL;
        p->recorderRecord = NULL;
        p->recorderBufferQueue = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (p->outputMixObject != NULL) {
        (*p->outputMixObject)->Destroy(p->outputMixObject);
        p->outputMixObject = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (p->engineObject != NULL) {
        (*p->engineObject)->Destroy(p->engineObject);
        p->engineObject = NULL;
        p->engineEngine = NULL;
    }

}
// open the android audio device for input and/or output
OPENSL_STREAM *android_OpenAudioDevice(int sr, int inchannels, int outchannels, int bufferframes){

    OPENSL_STREAM *p;
    p = (OPENSL_STREAM *) calloc(sizeof(OPENSL_STREAM),1);

    p->inchannels = inchannels;
    p->outchannels = outchannels;
    p->sr = sr;
    p->inlock = createThreadLock();
    p->outlock = createThreadLock();

    if((p->outBufSamples  =  bufferframes*outchannels) != 0) {
        if((p->outputBuffer[0] = (short *) calloc(p->outBufSamples, sizeof(short))) == NULL ||
           (p->outputBuffer[1] = (short *) calloc(p->outBufSamples, sizeof(short))) == NULL) {
            android_CloseAudioDevice(p);
            return NULL;
        }
    }

    if((p->inBufSamples  =  bufferframes*inchannels) != 0){
        if((p->inputBuffer[0] = (short *) calloc(p->inBufSamples, sizeof(short))) == NULL ||
           (p->inputBuffer[1] = (short *) calloc(p->inBufSamples, sizeof(short))) == NULL){
            android_CloseAudioDevice(p);
            return NULL;
        }
    }

    p->currentInputIndex = 0;
    p->currentOutputBuffer  = 0;
    p->currentInputIndex = p->inBufSamples;
    p->currentInputBuffer = 0;

    if(openSLCreateEngine(p) != SL_RESULT_SUCCESS) {
        android_CloseAudioDevice(p);
        return NULL;
    }


    if(openSLPlayOpen(p) != SL_RESULT_SUCCESS) {
        android_CloseAudioDevice(p);
        return NULL;
    }

    notifyThreadLock(p->outlock);
    notifyThreadLock(p->inlock);

    p->time = 0.;
    return p;
}
// close the android audio device
void android_CloseAudioDevice(OPENSL_STREAM *p){

    if (p == NULL)
        return;


    openSLDestroyEngine(p);

    if (p->inlock != NULL) {
        notifyThreadLock(p->inlock);
        destroyThreadLock(p->inlock);
        p->inlock = NULL;
    }

    if (p->outlock != NULL) {
        notifyThreadLock(p->outlock);
        destroyThreadLock(p->outlock);
        p->inlock = NULL;
    }

    if (p->outputBuffer[0] != NULL) {
        free(p->outputBuffer[0]);
        p->outputBuffer[0] = NULL;
    }

    if (p->outputBuffer[1] != NULL) {
        free(p->outputBuffer[1]);
        p->outputBuffer[1] = NULL;
    }

    if (p->inputBuffer[0] != NULL) {
        free(p->inputBuffer[0]);
        p->inputBuffer[0] = NULL;
    }

    if (p->inputBuffer[1] != NULL) {
        free(p->inputBuffer[1]);
        p->inputBuffer[1] = NULL;
    }

    free(p);
}
// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    OPENSL_STREAM *p = (OPENSL_STREAM *) context;
    notifyThreadLock(p->outlock);
}
// puts a buffer of size samples to the device
int android_AudioOut(OPENSL_STREAM *p, float *buffer,int size){

    short *outBuffer;
    int i, bufsamps = p->outBufSamples, index = p->currentOutputIndex;
    if(p == NULL  || bufsamps ==  0)  return 0;
    outBuffer = p->outputBuffer[p->currentOutputBuffer];

    for(i=0; i < size; i++){
        outBuffer[index++] = (short) (buffer[i]*CONV16BIT);
        if (index >= p->outBufSamples) {
            waitThreadLock(p->outlock);
            (*p->bqPlayerBufferQueue)->Enqueue(p->bqPlayerBufferQueue,
                                               outBuffer,bufsamps*sizeof(short));
            p->currentOutputBuffer = (p->currentOutputBuffer ?  0 : 1);
            index = 0;
            outBuffer = p->outputBuffer[p->currentOutputBuffer];
        }
    }
    p->currentOutputIndex = index;
    p->time += (double) size/(p->sr*p->outchannels);
    return i;
}



//----------------------------------------------------------------------
// thread Locks
// to ensure synchronisation between callbacks and processing code
void* createThreadLock(void)
{
    threadLock  *p;
    p = (threadLock*) malloc(sizeof(threadLock));
    if (p == NULL)
        return NULL;
    memset(p, 0, sizeof(threadLock));
    if (pthread_mutex_init(&(p->m), (pthread_mutexattr_t*) NULL) != 0) {
        free((void*) p);
        return NULL;
    }
    if (pthread_cond_init(&(p->c), (pthread_condattr_t*) NULL) != 0) {
        pthread_mutex_destroy(&(p->m));
        free((void*) p);
        return NULL;
    }
    p->s = (unsigned char) 1;

    return p;
}

int waitThreadLock(void *lock)
{
    threadLock  *p;

    int retval = 0;
    p = (threadLock*) lock;
    pthread_mutex_lock(&(p->m));
    while (!p->s) {
        pthread_cond_wait(&(p->c), &(p->m));
    }
    p->s = (unsigned char) 0;
    pthread_mutex_unlock(&(p->m));
    return retval;
}

void notifyThreadLock(void *lock)
{
    threadLock *p;
    p = (threadLock*) lock;
    pthread_mutex_lock(&(p->m));
    p->s = (unsigned char) 1;
    pthread_cond_signal(&(p->c));
    pthread_mutex_unlock(&(p->m));
}

void destroyThreadLock(void *lock)
{
    threadLock  *p;
    p = (threadLock*) lock;
    if (p == NULL)
        return;
    notifyThreadLock(p);
    pthread_cond_destroy(&(p->c));
    pthread_mutex_destroy(&(p->m));
    free(p);
}

/////////////////////////////////////OPEN_SL_DONE

static int on;
std::queue<MidiEvent> events;
#include <mutex>
std::mutex mutex;

bool isRecording;
bool isStopped;
std::queue<float> recordedData;
float params[52];
extern "C"
{
JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_startProcess(JNIEnv*, jobject ,jint sampleRate, jint frames);

JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_stopProcess(JNIEnv*, jobject);

JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setMidiMessage(JNIEnv* ,jobject ,jint message, jint note, jint vel);

JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setParameter(JNIEnv* ,jobject ,jint id, jfloat value);
JNIEXPORT float JNICALL
        Java_com_group3_synthesizerapp_MainActivity_getParameter(JNIEnv* ,jobject ,jint id);

JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setRecord(JNIEnv* ,jobject ,jboolean jboolean1);
JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setStopped(JNIEnv* ,jobject ,jboolean jboolean1);
JNIEXPORT jshortArray JNICALL
        Java_com_group3_synthesizerapp_MainActivity_getRecordedData(JNIEnv* env,jobject);

}

float FramesForPortamento = 0.f;
JNIEXPORT void JNICALL Java_com_group3_synthesizerapp_MainActivity_startProcess(JNIEnv* , jobject ,jint sampleRate, jint frames) {
    OPENSL_STREAM  *p;
    float outbuffer[frames];
    synthesizerModel = new SynthesizerModel();
    synthesizerModel->setSampleRate(sampleRate);
    p = android_OpenAudioDevice(sampleRate,0,2,frames);

    isRecording = false;
    isStopped = false;
    if(p == NULL) return;
    on = 1;
    FramesForPortamento = frames;
    int f = 32;
    while(on)
    {

        for(int i = 1; i <= frames;i++)
        {
            if (f % i == 0)
            {

                synthesizerModel->processEvenets(events, 0);
                synthesizerModel->processReplacing(outbuffer, f);

                if (isRecording)
                {
                    for (int j = 0; j < f; j++)
                    {
                        recordedData.push(outbuffer[j]);
                    }
                }

                if (isStopped)
                {
                    isRecording = false;
                    isStopped = false;
                }

                android_AudioOut(p, outbuffer, f);
            }
        }
    }
    android_CloseAudioDevice(p);
    delete synthesizerModel;
}

JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setRecord(JNIEnv* ,jobject ,jboolean jboolean1)
{
    isRecording = jboolean1;
}
JNIEXPORT void JNICALL
        Java_com_group3_synthesizerapp_MainActivity_setStopped(JNIEnv* ,jobject ,jboolean jboolean1)
{
    isStopped = jboolean1;
}
JNIEXPORT jshortArray JNICALL
        Java_com_group3_synthesizerapp_MainActivity_getRecordedData(JNIEnv* env,jobject)
{
    jsize recordSize = (jsize)recordedData.size();
    jshortArray result;
    result = (env)->NewShortArray(recordSize);

    if (result == NULL) {
        return NULL;
        /* out of memory error thrown */
    }

    // fill a temp structure to use to populate the java int array
    jshort fill[recordSize];

    for (jsize i = 0; i < recordSize; i++) {
        fill[i] = (jshort)(SHRT_MAX*recordedData.front());
        recordedData.pop();
    }

    // move from the temp structure to the java structure
    (env)->SetShortArrayRegion(result, 0,recordSize, fill);

    return result;
}

JNIEXPORT void JNICALL Java_com_group3_synthesizerapp_MainActivity_stopProcess(JNIEnv*,jobject){
    on = 0;
}

JNIEXPORT void JNICALL Java_com_group3_synthesizerapp_MainActivity_setMidiMessage(JNIEnv* ,jobject ,jint message, jint note, jint vel)
{
    MidiEvent event;
    event.message = static_cast<int> (message);
    event.note = static_cast<int>(note);
    event.velocity = static_cast<int>(vel);
    events.push(event);
}
float mapping(float mMax,float mMin, float mParam)
{
    float logmax = log10f( mMax );
    float logmin = log10f( mMin );
    float logdata = (mParam * (logmax-logmin)) + logmin;
    float  mData = powf( 10.0f, logdata );

    if (mData < mMin)
    {
        mData = mMin;
    }

    if (mData > mMax)
    {
        mData = mMax;
    }
    return mData;
}

JNIEXPORT float JNICALL
        Java_com_group3_synthesizerapp_MainActivity_getParameter(JNIEnv* ,jobject ,jint id)
{
    return params[id-100];
}

JNIEXPORT void JNICALL Java_com_group3_synthesizerapp_MainActivity_setParameter(JNIEnv* ,jobject ,jint id,  jfloat value)
{

    params[id-100] = value;
    if(id>=100 && id<=115)
    {
        value *= 3.162277660168379f;
    }

    switch (id) {
        case 100:
            synthesizerModel->mOsc[0].mInputAmp[0] = value;
            break;
        case 101:
            synthesizerModel->mOsc[0].mInputAmp[1] = value;
            break;
        case 102:
            synthesizerModel->mOsc[0].mInputAmp[2] = value;
            break;
        case 103:
            synthesizerModel->mOsc[0].mInputAmp[3] = value;
            break;
        case 104:
            synthesizerModel->mOsc[1].mInputAmp[0] = value;
            break;
        case 105:
            synthesizerModel->mOsc[1].mInputAmp[1] = value;
            break;
        case 106:
            synthesizerModel->mOsc[1].mInputAmp[2] = value;
            break;
        case 107:
            synthesizerModel->mOsc[1].mInputAmp[3] = value;
            break;
        case 108:
            synthesizerModel->mOsc[2].mInputAmp[0] = value;
            break;
        case 109:
            synthesizerModel->mOsc[2].mInputAmp[1] = value;
            break;
        case 110:
            synthesizerModel->mOsc[2].mInputAmp[2] = value;
            break;
        case 111:
            synthesizerModel->mOsc[2].mInputAmp[3] = value;
            break;
        case 112:
            synthesizerModel->mOsc[3].mInputAmp[0] = value;
            break;
        case 113:
            synthesizerModel->mOsc[3].mInputAmp[1] = value;
            break;
        case 114:
            synthesizerModel->mOsc[3].mInputAmp[2] = value;
            break;
        case 115:
            synthesizerModel->mOsc[3].mInputAmp[3] = value;
            break;
        case 116:
            synthesizerModel->mOutput[0] = value;
            break;
        case 117:
            synthesizerModel->mOutput[1] = value;
            break;
        case 118:
            synthesizerModel->mOutput[2] = value;
            break;
        case 119:
            synthesizerModel->mOutput[3] = value;
            break;
        case 120:
            synthesizerModel->mPan[0] = value;
            break;
        case 121:
            synthesizerModel->mPan[1] = value;
            break;
        case 122:
            synthesizerModel->mPan[2] = value;
            break;
        case 123:
            synthesizerModel->mPan[3] = value;
            break;
        case 124:
            synthesizerModel->mSemi[0] = floorf(-24+value*48.f);
            break;
        case 125:
            synthesizerModel->mSemi[1] = floorf(-24+value*48.f);
            break;
        case 126:
            synthesizerModel->mSemi[2] = floorf(-24+value*48.f);
            break;
        case 127:
            synthesizerModel->mSemi[3] = floorf(-24+value*48.f);
            break;
        case 128:
            synthesizerModel->mFine[0]= -1.f+(value*2.f);
            break;
        case 129:
            synthesizerModel->mFine[1]= -1.f+(value*2.f);
            break;
        case 130:
            synthesizerModel->mFine[2]= -1.f+(value*2.f);
            break;
        case 131:
            synthesizerModel->mFine[3]= -1.f+(value*2.f);
            break;
        case 132:
            synthesizerModel->mADSR[0].setAttackRate(mapping(10.f,0.001f,value));
            break;
        case 133:
            synthesizerModel->mADSR[0].setDecayRate(mapping(10.f,0.001f,value));
            break;
        case 134:
            synthesizerModel->mADSR[0].setSustainLevel(value);
            break;
        case 135:
            synthesizerModel->mADSR[0].setReleaseRate(mapping(10.f,0.001f,value));
            break;
        case 136:
            synthesizerModel->mADSR[1].setAttackRate(mapping(10.f,0.001f,value));
            break;
        case 137:
            synthesizerModel->mADSR[1].setDecayRate(mapping(10.f,0.001f,value));
            break;
        case 138:
            synthesizerModel->mADSR[1].setSustainLevel(value);
            break;
        case 139:
            synthesizerModel->mADSR[1].setReleaseRate(mapping(10.f,0.001f,value));
            break;
        case 140:
            synthesizerModel->mADSR[2].setAttackRate(mapping(10.f,0.001f,value));
            break;
        case 141:
            synthesizerModel->mADSR[2].setDecayRate(mapping(10.f,0.001f,value));
            break;
        case 142:
            synthesizerModel->mADSR[2].setSustainLevel(value);
            break;
        case 143:
            synthesizerModel->mADSR[2].setReleaseRate(mapping(10.f,0.001f,value));
            break;
        case 144:
            synthesizerModel->mADSR[3].setAttackRate(mapping(10.f,0.001f,value));
            break;
        case 145:
            synthesizerModel->mADSR[3].setDecayRate(mapping(10.f,0.001f,value));
            break;
        case 146:
            synthesizerModel->mADSR[3].setSustainLevel(value);
            break;
        case 147:
            synthesizerModel->mADSR[3].setReleaseRate(mapping(10.f,0.001f,value));
            break;
        case 148:
            synthesizerModel->mMaster = value * VOICES;
            break;
        case 149:
            synthesizerModel->port = 1.0f/((value*((float)synthesizerModel->mSampleRate-1.0f))+1.0f);
            break;
        case 150:
            synthesizerModel->mono = (int)floorf(value+.5f);
            break;
    }
}

