package me.nald.blog.data.persistence.entity;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.nald.blog.data.vo.YN;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@ToString( exclude = "fileCover")
public class Storage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_seq", nullable = false)
    private Long storageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "download_src", nullable = false)
    private String downloadSrc;

    @Column(name = "file_src", nullable = false)
    private String fileSrc;

    @Column(name = "vtt_src", nullable = false)
    private String vttSrc;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false)
    private String fileType;

//    @Lob
//    @Column(name = "file_cover", nullable = false, columnDefinition="LONGTEXT")
    @Column(name = "file_cover", nullable = false)
    private String fileCover;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_auth", nullable = false, length = 1)
    @ColumnDefault("'N'")
    private YN fileAuth;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_download", nullable = false, length = 1)
    @ColumnDefault("'N'")
    private YN fileDownload;

    @Column(name = "created_dt", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private Timestamp createdDt;

    public static Storage createStorage(String fileName,
                                        Long fileSize,
                                        String downloadSrc,
                                        String fileType,
                                        String fileCover,
                                        String vttSrc,
                                        YN fileAuth,
                                        YN fileDownload) {
        Storage storage = new Storage();
        storage.setFileName(fileName);
        storage.setFileSize(fileSize);
        storage.setDownloadSrc(downloadSrc);
        storage.setFileCover(fileCover);
        storage.setFileType(fileType);
        storage.setVttSrc(vttSrc);
        storage.setFileAuth(fileAuth);
        storage.setFileDownload(fileDownload);
        return storage;
    }

}

