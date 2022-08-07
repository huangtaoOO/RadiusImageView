/*
 * Tencent is pleased to support the open source community by making QMUI_Android available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tao.radius;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 提供为图片添加圆角、边框、剪裁到圆形或其他形状等功能。
 */
public class RadiusImageView extends AppCompatImageView {
    private static final int DEFAULT_BORDER_COLOR = Color.GRAY;

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static final int COLOR_DRAWABLE_DIMEN = 2;

    private boolean mIsSelected = false;
    private boolean mIsOval = false;
    private boolean mIsCircle = false;

    private int mBorderWidth;
    private int mBorderColor;

    private int mSelectedBorderWidth;
    private int mSelectedBorderColor;
    private int mSelectedMaskColor;
    private boolean mIsTouchSelectModeEnabled = true;

    private int mCornerRadius;
    private boolean roundTopLeft = true,roundTopRight= true,
            roundBottomRight= true,roundBottomLeft= true;

    private Paint mBitmapPaint;
    private Paint mBorderPaint;
    private ColorFilter mColorFilter;
    private ColorFilter mSelectedColorFilter;
    private BitmapShader mBitmapShader;
    private boolean mNeedResetShader = false;

    private RectF mRectF = new RectF();
    private RectF mDrawRectF = new RectF();
    private Path path = null;

    private Bitmap mBitmap;

    private Matrix mMatrix;
    private int mWidth;
    private int mHeight;
    private ScaleType mLastCalculateScaleType;

    @IdRes
    private int placeholderImage;

    public RadiusImageView(Context context) {
        this(context, null, R.attr.RadiusImageViewStyle);
    }

    public RadiusImageView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.RadiusImageViewStyle);
    }

    public RadiusImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mMatrix = new Matrix();

        setScaleType(ScaleType.CENTER_CROP);

        TypedArray array = context.obtainStyledAttributes(
                attrs, R.styleable.RadiusImageView, defStyleAttr, 0);

        mBorderWidth = array.getDimensionPixelSize(R.styleable.RadiusImageView_borderWidth, 0);
        mBorderColor = array.getColor(R.styleable.RadiusImageView_borderColor, DEFAULT_BORDER_COLOR);
        mSelectedBorderWidth = array.getDimensionPixelSize(
                R.styleable.RadiusImageView_selectedBorderWidth, mBorderWidth);
        mSelectedBorderColor = array.getColor(
                R.styleable.RadiusImageView_selectedBorderColor, mBorderColor);
        mSelectedMaskColor = array.getColor(
                R.styleable.RadiusImageView_selectedMaskColor, Color.TRANSPARENT);
        if (mSelectedMaskColor != Color.TRANSPARENT) {
            mSelectedColorFilter = new PorterDuffColorFilter(mSelectedMaskColor, PorterDuff.Mode.DARKEN);
        }

        mIsTouchSelectModeEnabled = array.getBoolean(
                R.styleable.RadiusImageView_isTouchSelectModeEnabled, true);
        mIsCircle = array.getBoolean(R.styleable.RadiusImageView_isCircle, false);
        if (!mIsCircle) {
            mIsOval = array.getBoolean(R.styleable.RadiusImageView_isOval, false);
        }
        if (!mIsOval) {
            mCornerRadius = array.getDimensionPixelSize(
                    R.styleable.RadiusImageView_cornerRadius, 0);
            roundTopLeft = array.getBoolean(R.styleable.RadiusImageView_roundTopLeft,true);
            roundTopRight = array.getBoolean(R.styleable.RadiusImageView_roundTopRight,true);
            roundBottomRight = array.getBoolean(R.styleable.RadiusImageView_roundBottomRight,true);
            roundBottomLeft = array.getBoolean(R.styleable.RadiusImageView_roundBottomLeft,true);
        }

        placeholderImage = array.getResourceId(R.styleable.RadiusImageView_placeholderImage,0);
        array.recycle();
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (adjustViewBounds) {
            throw new IllegalArgumentException("不支持adjustViewBounds");
        }
    }

    /**
     * 设置边框宽度
     * @param borderWidth borderWidth
     */
    public void setBorderWidth(int borderWidth) {
        if (mBorderWidth != borderWidth) {
            mBorderWidth = borderWidth;
            invalidate();
        }
    }

    /**
     * 设置边框颜色
     * @param borderColor borderColor
     */
    public void setBorderColor(@ColorInt int borderColor) {
        if (mBorderColor != borderColor) {
            mBorderColor = borderColor;
            invalidate();
        }
    }

    /**
     * 设置角半径
     * @param cornerRadius cornerRadius
     */
    public void setCornerRadius(int cornerRadius) {
        if (mCornerRadius != cornerRadius) {
            mCornerRadius = cornerRadius;
            if (!mIsCircle && !mIsOval) {
                invalidate();
            }
        }
    }

    /**
     * 设置选定边框颜色
     * @param selectedBorderColor selectedBorderColor
     */
    public void setSelectedBorderColor(@ColorInt int selectedBorderColor) {
        if (mSelectedBorderColor != selectedBorderColor) {
            mSelectedBorderColor = selectedBorderColor;
            if (mIsSelected) {
                invalidate();
            }
        }

    }

    /**
     * 设置选定的边框宽度
     * @param selectedBorderWidth selectedBorderWidth
     */
    public void setSelectedBorderWidth(int selectedBorderWidth) {
        if (mSelectedBorderWidth != selectedBorderWidth) {
            mSelectedBorderWidth = selectedBorderWidth;
            if (mIsSelected) {
                invalidate();
            }
        }
    }

    /**
     * 设置选定的遮罩颜色
     * @param selectedMaskColor selectedMaskColor
     */
    public void setSelectedMaskColor(@ColorInt int selectedMaskColor) {
        if (mSelectedMaskColor != selectedMaskColor) {
            mSelectedMaskColor = selectedMaskColor;
            if (mSelectedMaskColor != Color.TRANSPARENT) {
                mSelectedColorFilter = new PorterDuffColorFilter(mSelectedMaskColor, PorterDuff.Mode.DARKEN);
            } else {
                mSelectedColorFilter = null;
            }
            if (mIsSelected) {
                invalidate();
            }
        }
        mSelectedMaskColor = selectedMaskColor;
    }


    /**
     * 是否是圆
     * @param isCircle 是否
     */
    public void setCircle(boolean isCircle) {
        if (mIsCircle != isCircle) {
            mIsCircle = isCircle;
            requestLayout();
            invalidate();
        }
    }

    /**
     * 是否是椭圆
     * @param isOval 是否
     */
    public void setOval(boolean isOval) {
        boolean forceUpdate = false;
        if (isOval) {
            if (mIsCircle) {
                // 必须先取消圆形
                mIsCircle = false;
                forceUpdate = true;
            }

        }
        if (mIsOval != isOval || forceUpdate) {
            mIsOval = isOval;
            requestLayout();
            invalidate();
        }
    }

    /**
     * 设置圆角的位置
     * @param roundTopLeft roundTopLeft
     * @param roundTopRight roundTopRight
     * @param roundBottomRight roundBottomRight
     * @param roundBottomLeft roundBottomLeft
     */
    public void setRoundedLocation(boolean roundTopLeft,boolean roundTopRight,
                                   boolean roundBottomRight,boolean roundBottomLeft){
        if (this.roundTopLeft!=roundTopLeft || this.roundTopRight!=roundTopRight
                || this.roundBottomLeft!=roundBottomLeft || this.roundBottomRight!=roundBottomRight){
            this.roundTopLeft = roundTopLeft;
            this.roundTopRight = roundTopRight;
            this.roundBottomRight = roundBottomRight;
            this.roundBottomLeft = roundBottomLeft;
            requestLayout();
            invalidate();
        }

    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public int getCornerRadius() {
        return mCornerRadius;
    }

    public int getSelectedBorderColor() {
        return mSelectedBorderColor;
    }

    public int getSelectedBorderWidth() {
        return mSelectedBorderWidth;
    }

    public int getSelectedMaskColor() {
        return mSelectedMaskColor;
    }


    public boolean isCircle() {
        return mIsCircle;
    }

    public boolean isOval() {
        return !mIsCircle && mIsOval;
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    @Override
    public void setSelected(boolean isSelected) {
        if (mIsSelected != isSelected) {
            mIsSelected = isSelected;
            invalidate();
        }
    }

    public void setTouchSelectModeEnabled(boolean touchSelectModeEnabled) {
        mIsTouchSelectModeEnabled = touchSelectModeEnabled;
    }

    public boolean isTouchSelectModeEnabled() {
        return mIsTouchSelectModeEnabled;
    }

    public void setSelectedColorFilter(ColorFilter cf) {
        if (mSelectedColorFilter == cf) {
            return;
        }
        mSelectedColorFilter = cf;
        if (mIsSelected) {
            invalidate();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mColorFilter == cf) {
            return;
        }
        mColorFilter = cf;
        if (!mIsSelected) {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize);
            return;
        }
        if (mIsCircle) {
            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(widthSize, widthSize);
            } else if (heightMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(heightSize, heightSize);
            } else {
                if (mBitmap == null) {
                    setMeasuredDimension(0, 0);
                } else {
                    int w = Math.min(mBitmap.getWidth(), widthSize);
                    int h = Math.min(mBitmap.getHeight(), heightSize);
                    int size = Math.min(w, h);
                    setMeasuredDimension(size, size);
                }
            }
            return;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        setupBitmap();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        setupBitmap();
    }

    private Bitmap getBitmap() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap == null) {
                return null;
            }
            float bmWidth = bitmap.getWidth(), bmHeight = bitmap.getHeight();
            if (bmWidth == 0 || bmHeight == 0) {
                return null;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // ensure minWidth and minHeight
                float minScaleX = getMinimumWidth() / bmWidth, minScaleY = getMinimumHeight() / bmHeight;
                if (minScaleX > 1 || minScaleY > 1) {
                    float scale = Math.max(minScaleX, minScaleY);
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);

                    return Bitmap.createBitmap(
                            bitmap, 0, 0, (int) bmWidth, (int) bmHeight, matrix, false);
                }
            }
            return bitmap;
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLOR_DRAWABLE_DIMEN, COLOR_DRAWABLE_DIMEN, BITMAP_CONFIG);
            } else {
                bitmap = Bitmap.createBitmap(
                        drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), BITMAP_CONFIG);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void setupBitmap() {
        Bitmap bm = getBitmap();
        if (bm == mBitmap) {
            return;
        }
        mBitmap = bm;
        if (mBitmap == null) {
            mBitmapShader = null;
            invalidate();
            return;
        }
        mNeedResetShader = true;
        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        if (mBitmapPaint == null) {
            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
        }
        mBitmapPaint.setShader(mBitmapShader);
        requestLayout();
        invalidate();
    }

    private void updateBitmapShader() {
        mMatrix.reset();
        mNeedResetShader = false;
        if (mBitmapShader == null || mBitmap == null) {
            return;
        }
        updateMatrix(mMatrix, mBitmap, mRectF);
        mBitmapShader.setLocalMatrix(mMatrix);
        mBitmapPaint.setShader(mBitmapShader);
    }

    private void updateMatrix(@NonNull Matrix matrix, @NonNull Bitmap bitmap, RectF drawRect) {
        final float bmWidth = bitmap.getWidth();
        final float bmHeight = bitmap.getHeight();
        final ScaleType scaleType = getScaleType();
        if (scaleType == ScaleType.MATRIX) {
            updateScaleTypeMatrix(matrix, bitmap, drawRect);
        } else if (scaleType == ScaleType.CENTER) {
            float left = (mWidth - bmWidth) / 2;
            float top = (mHeight - bmHeight) / 2;
            matrix.postTranslate(left, top);
            drawRect.set(
                    Math.max(0, left),
                    Math.max(0, top),
                    Math.min(left + bmWidth, mWidth),
                    Math.min(top + bmHeight, mHeight));
        } else if (scaleType == ScaleType.CENTER_CROP) {
            float scaleX = mWidth / bmWidth, scaleY = mHeight / bmHeight;
            final float scale = Math.max(scaleX, scaleY);
            matrix.setScale(scale, scale);
            matrix.postTranslate(-(scale * bmWidth - mWidth) / 2, -(scale * bmHeight - mHeight) / 2);
            drawRect.set(0, 0, mWidth, mHeight);
        } else if (scaleType == ScaleType.CENTER_INSIDE) {
            float scaleX = mWidth / bmWidth, scaleY = mHeight / bmHeight;
            if (scaleX >= 1 && scaleY >= 1) {
                float left = (mWidth - bmWidth) / 2;
                float top = (mHeight - bmHeight) / 2;
                matrix.postTranslate(left, top);
                drawRect.set(left, top, left + bmWidth, top + bmHeight);
            } else {
                float scale = Math.min(scaleX, scaleY);
                matrix.setScale(scale, scale);
                float bw = bmWidth * scale, bh = bmHeight * scale;
                float left = (mWidth - bw) / 2;
                float top = (mHeight - bh) / 2;
                matrix.postTranslate(left, top);
                drawRect.set(left, top, left + bw, top + bh);
            }
        } else if (scaleType == ScaleType.FIT_XY) {
            float scaleX = mWidth / bmWidth, scaleY = mHeight / bmHeight;
            matrix.setScale(scaleX, scaleY);
            drawRect.set(0, 0, mWidth, mHeight);
        } else {
            float scaleX = mWidth / bmWidth, scaleY = mHeight / bmHeight;
            float scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);
            float bw = bmWidth * scale, bh = bmHeight * scale;
            if (scaleType == ScaleType.FIT_START) {
                drawRect.set(0, 0, bw, bh);
            } else if (scaleType == ScaleType.FIT_CENTER) {
                float left = (mWidth - bw) / 2;
                float top = (mHeight - bh) / 2;
                matrix.postTranslate(left, top);
                drawRect.set(left, top, left + bw, top + bh);
            } else {
                matrix.postTranslate(mWidth - bw, mHeight - bh);
                drawRect.set(mWidth - bw, mHeight - bh, mWidth, mHeight);
            }
        }

    }

    protected void updateScaleTypeMatrix(@NonNull Matrix matrix, @NonNull Bitmap bitmap, RectF drawRect) {
        matrix.set(getImageMatrix());
        drawRect.set(0, 0, mWidth, mHeight);
    }

    private void drawBitmap(Canvas canvas, int borderWidth) {
        final float halfBorderWidth = borderWidth * 1.0f / 2;
        mBitmapPaint.setColorFilter(mIsSelected ? mSelectedColorFilter : mColorFilter);

        if (mIsCircle) {
            canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), (Math.min(mRectF.width() / 2, mRectF.height() / 2)) - halfBorderWidth, mBitmapPaint);
        } else {
            mDrawRectF.left = mRectF.left + halfBorderWidth;
            //noinspection SuspiciousNameCombination
            mDrawRectF.top = mRectF.top + halfBorderWidth;
            mDrawRectF.right = mRectF.right - halfBorderWidth;
            mDrawRectF.bottom = mRectF.bottom - halfBorderWidth;
            if (mIsOval) {
                canvas.drawOval(mDrawRectF, mBitmapPaint);
            } else {
                if (path == null){
                    path = new Path();
                }
                path.addRoundRect(
                        mDrawRectF,
                        new float[] {
                                roundTopLeft?mCornerRadius:0,
                                roundTopLeft?mCornerRadius:0,
                                roundTopRight?mCornerRadius:0,
                                roundTopRight?mCornerRadius:0,
                                roundBottomRight?mCornerRadius:0,
                                roundBottomRight?mCornerRadius:0,
                                roundBottomLeft?mCornerRadius:0,
                                roundBottomLeft?mCornerRadius:0
                        },
                        Path.Direction.CW);
                canvas.drawPath(path, mBitmapPaint);
            }
        }
    }

    private void drawBorder(Canvas canvas, int borderWidth) {
        if (borderWidth <= 0) {
            return;
        }
        final float halfBorderWidth = borderWidth * 1.0f / 2;
        mBorderPaint.setColor(mIsSelected ? mSelectedBorderColor : mBorderColor);
        mBorderPaint.setStrokeWidth(borderWidth);
        if (mIsCircle) {
            canvas.drawCircle(mRectF.centerX(), mRectF.centerY(),
                    Math.min(mRectF.width(), mRectF.height()) / 2 - halfBorderWidth, mBorderPaint);
        } else {
            mDrawRectF.left = mRectF.left + halfBorderWidth;
            //noinspection SuspiciousNameCombination
            mDrawRectF.top = mRectF.top + halfBorderWidth;
            mDrawRectF.right = mRectF.right - halfBorderWidth;
            mDrawRectF.bottom = mRectF.bottom - halfBorderWidth;
            if (mIsOval) {
                canvas.drawOval(mDrawRectF, mBorderPaint);
            } else {
                if (path == null){
                    path = new Path();
                }
                path.addRoundRect(
                        mDrawRectF,
                        new float[] {
                                roundTopLeft?mCornerRadius:0,
                                roundTopLeft?mCornerRadius:0,
                                roundTopRight?mCornerRadius:0,
                                roundTopRight?mCornerRadius:0,
                                roundBottomRight?mCornerRadius:0,
                                roundBottomRight?mCornerRadius:0,
                                roundBottomLeft?mCornerRadius:0,
                                roundBottomLeft?mCornerRadius:0
                        },
                        Path.Direction.CW);
                canvas.drawPath(path, mBitmapPaint);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int borderWidth = mIsSelected ? mSelectedBorderWidth : mBorderWidth;

        if (mBitmap == null || mBitmapShader == null) {
            drawBorder(canvas, borderWidth);
            return;
        }

        if (mWidth != width || mHeight != height
                || mLastCalculateScaleType != getScaleType() || mNeedResetShader) {
            mWidth = width;
            mHeight = height;
            mLastCalculateScaleType = getScaleType();
            updateBitmapShader();
        }
        drawBitmap(canvas, borderWidth);
        drawBorder(canvas, borderWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.isClickable()) {
            this.setSelected(false);
            return super.onTouchEvent(event);
        }

        if (!mIsTouchSelectModeEnabled) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.setSelected(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                this.setSelected(false);
                break;
        }
        return super.onTouchEvent(event);
    }
}
