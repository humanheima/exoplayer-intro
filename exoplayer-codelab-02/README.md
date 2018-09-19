##  ExoPlayer basic use
[Codelab](https://codelabs.developers.google.com/codelabs/exoplayer-intro/index.html?index=..%2F..%2Findex#0)
MediaSource的四种分类
* DASH (DashMediaSource)
* SmoothStreaming (SsMediaSource)
* HLS (HlsMediaSource) 
* 常规媒体文件 (ExtractorMediaSource)

```java
/**
     * 只播放视频的一部分 比如从第10秒到30秒的部分
     *
     * @param uri 常规类型媒体文件 比如MP4
     * @return
     */
    private MediaSource buildMediaSource(Uri uri) {
        ExtractorMediaSource mp4MediaSource = new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                createMediaSource(uri);
        //单位是微秒
        ClippingMediaSource clippingMediaSource = new ClippingMediaSource(mp4MediaSource,
                40_000_000, 80_000_000);
        return clippingMediaSource;
    }
```