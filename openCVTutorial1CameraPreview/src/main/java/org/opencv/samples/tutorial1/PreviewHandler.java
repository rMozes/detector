package org.opencv.samples.tutorial1;


import org.opencv.core.Point;

/**
 * Created by richimozes on 6/6/16.
 */
public interface PreviewHandler {
    void onPreviewData(byte[] _data, Point _previewSize, int _format);
}
