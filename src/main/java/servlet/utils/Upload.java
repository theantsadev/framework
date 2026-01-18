package servlet.utils;

import jakarta.servlet.http.Part;
import java.io.*;

public class Upload {
    private Part part;
    private String uploadDirectory = "uploads";

    public Upload(Part part, String uploadDirectory) {
        this.part = part;
        this.uploadDirectory = uploadDirectory;
    }

    public String getFileName() {
        return part.getSubmittedFileName();
    }

    public long getSize() {
        return part.getSize();
    }

    public String getContentType() {
        return part.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    public void saveTo(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = directory + File.separator + getFileName();
        part.write(filePath);
    }

    public void save() throws IOException {
        saveTo(uploadDirectory);
    }

    public byte[] getBytes() throws IOException {
        try (InputStream is = getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    public Part getPart() {
        return part;
    }

    @Override
    public String toString() {
        return "Upload{fileName='" + getFileName() + "', size=" + getSize() + ", contentType='" + getContentType()
                + "'}";
    }
}