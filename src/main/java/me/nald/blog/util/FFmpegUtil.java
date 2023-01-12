//package me.nald.blog.util;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.bramp.ffmpeg.FFmpeg;
//import net.bramp.ffmpeg.FFmpegExecutor;
//import net.bramp.ffmpeg.FFprobe;
//import net.bramp.ffmpeg.builder.FFmpegBuilder;
//import net.bramp.ffmpeg.job.FFmpegJob;
//import net.bramp.ffmpeg.probe.FFmpegProbeResult;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FFmpegUtil {
//    private final FFmpeg ffMpeg;
//    private final FFprobe ffProbe;
//
//    private final ObjectMapper objectMapper;
//
//    @Value("${ffmpeg.convert.savepath}")
//    private String convertSavePath;
//
//    public FFmpegProbeResult getProbeResult(String filePath) {
//        FFmpegProbeResult ffmpegProbeResult;
//
//        try {
//            ffmpegProbeResult = ffProbe.probe(filePath);
//
//            System.out.println("비트레이트 : "+ffmpegProbeResult.getStreams().get(0).bit_rate);
//            System.out.println("채널 : "+ffmpegProbeResult.getStreams().get(0).channels);
//            System.out.println("코덱 명 : "+ffmpegProbeResult.getStreams().get(0).codec_name);
//            System.out.println("코덱 유형 : "+ffmpegProbeResult.getStreams().get(0).codec_type);
//            System.out.println("해상도(너비) : "+ffmpegProbeResult.getStreams().get(0).width);
//            System.out.println("해상도(높이) : "+ffmpegProbeResult.getStreams().get(0).height);
//            System.out.println("포맷(확장자) : "+ffmpegProbeResult.getFormat());
//
//        } catch (IOException e) {
//            log.error(e.toString());
//            throw new RuntimeException(e);
//        }
//
//        return ffmpegProbeResult;
//    }
//
//    /**
//     * 비디오 Prop을 변경시키는 로직
//     * @param probeResult
//     * @param format 영상 포맷 (확장자 ex - mp4, mpeg )
//     * @param codec 비디오 코덱
//     * @param audioChannel 오디오 채널 ( 1: 모노, 2: 스테레오 )
//     * @param width 해상도 ( 너비 )
//     * @param height 해상도 ( 높이 )
//     * @return 변환 결과
//     */
//    public boolean convertVideoProp(FFmpegProbeResult probeResult, String format, String codec, int audioChannel, int width, int height) {
//        boolean result = false;
//
//        FFmpegBuilder builder = new FFmpegBuilder().setInput(probeResult)
//                .overrideOutputFiles(true)
//                .addOutput(convertSavePath + "/temp."+format)
//                .setFormat(format)
//                .setVideoCodec(codec)
//                .setAudioChannels(audioChannel)
//                .setVideoResolution(width, height)
//                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
//                .done();
//
////        StopWatch stopWatch = new StopWatch("convertVideoTimer");
//
//        FFmpegExecutor executable = new FFmpegExecutor(ffMpeg, ffProbe);
//        FFmpegJob job = executable.createJob(builder);
//        job.run();
//
//        if (job.getState() == FFmpegJob.State.FINISHED) {
//            result = true;
//        }
//
//        return result;
//    }
//
//}