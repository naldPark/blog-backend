package me.nald.blog.service;

import lombok.RequiredArgsConstructor;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.util.FileUtils;
import me.nald.blog.util.Util;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.Folder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StorageService {

    private static BlogProperties blogProperties;

    @Autowired
    public void setBlogProperties(BlogProperties blogProperties) {
        this.blogProperties = blogProperties;
    }


    public Path postWorkspaceDownloads() {
        Path zipFilePath = Paths.get("/naldstorage").resolve("sample." + "mp4");

        return zipFilePath;
    }

    public Boolean videoHls(String movieName) {
        Boolean result = false;
        FFmpeg ffmpeg = null;
        FFprobe ffprobe = null;
        String movieDir = blogProperties.getCommonPath() + "/movie";
        String fileName = FilenameUtils.getBaseName(movieName);
        String inputPath = movieDir + "/upload/";
        String HlsPath = movieDir + "/hls/" + fileName + "/";

        try {
            String osName = System.getProperty("os.name");
//            if (osName.toLowerCase().contains("unix") || osName.toLowerCase().contains("linux")) {
                File folder = new File(HlsPath);
                if (!folder.exists()) {
                    folder.mkdir(); //폴더 생성합니다.
                }
                ffmpeg = new FFmpeg(blogProperties.getFfmpegPath() + "/ffmpeg");
                ffprobe = new FFprobe(blogProperties.getFfmpegPath() + "/ffprobe");
//            } else {
//                ffmpeg = new FFmpeg("mpeg/ffmpeg");
//                ffprobe = new FFprobe("mpeg/ffprobe");
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)
                .setInput(inputPath + movieName)
                .addOutput(HlsPath + fileName + ".m3u8")
//                .addExtraArgs("-acodec", "copy")
//                .addExtraArgs("-vcodec", "copy")
                .addExtraArgs("-profile:v", "baseline")
                .addExtraArgs("-level", "3.0")
                .addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-f", "hls")
                .addExtraArgs("-safe", "0")
                .addExtraArgs("-preset", "ultrafast")

                .setVideoResolution(640, 480)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        builder.setVerbosity(FFmpegBuilder.Verbosity.DEBUG);

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        job.run();
        if (job.getState() == FFmpegJob.State.FINISHED) {
            result = true;
        }


        return result;
    }

}

