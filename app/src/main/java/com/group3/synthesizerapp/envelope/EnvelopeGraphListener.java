package com.group3.synthesizerapp.envelope;

import android.view.View;

/**
 * Created by Marvin on 10/29/2016.
 */

public interface EnvelopeGraphListener {
    enum Segment{
        kAttackX(0),
        kAttackY(1),
        kAttackShape(2),
        kDecayX(3),
        kDecayShape(4),
        kSustainY(5),
        kReleaseX(6),
        kReleaseShape(7),
        kNumberOfSegments(8);

        private final int value;
        Segment(final int value)
        {
            this.value = value;
        }

        public final int getValue()
        {
            return value;
        }

    }
    void OnEnvelopeGraphChanged(View view, Segment segement, float value);
}
