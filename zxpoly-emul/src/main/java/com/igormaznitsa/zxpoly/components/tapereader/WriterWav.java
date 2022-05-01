package com.igormaznitsa.zxpoly.components.tapereader;

import java.io.*;
import java.util.Objects;

public final class WriterWav {

    public static final class WavFile {

        private final OutputStream outputStream;
        private final WriterWav writerWav;
        private final File file;

        public WavFile(final File file) throws IOException {
            this.file = file;
            this.outputStream = new BufferedOutputStream(new FileOutputStream(file));
            this.writerWav = new WriterWav(this.outputStream);
        }

        public WavFile header(
                final int audioFormat,
                final int numChannels,
                final int sampleRate,
                final int byteRate,
                final int blockAlign,
                final int bitsPerSample,
                final int... extraParams
        ) throws IOException {
            this.writerWav.header(audioFormat, numChannels, sampleRate, byteRate, blockAlign, bitsPerSample, extraParams);
            return this;
        }

        public WavFile data(final int data) throws IOException {
            this.writerWav.data(data);
            return this;
        }

        public WavFile data(final byte... data) throws IOException {
            this.writerWav.data(data);
            return this;
        }

        public WavFile data(final byte[] data, final int offset, final int length) throws IOException {
            this.writerWav.data(data, offset, length);
            return this;
        }

        private static void writeInt(final DataOutput dataOutput, final int value) throws IOException {
            dataOutput.write(value);
            dataOutput.write(value >> 8);
            dataOutput.write(value >> 16);
            dataOutput.write(value >> 24);
        }

        public void close() throws IOException {
            this.outputStream.flush();
            this.outputStream.close();
            final int size = (int) this.writerWav.getLength();

            try (final RandomAccessFile randomAccessFile = new RandomAccessFile(this.file, "rw")) {
                randomAccessFile.seek(this.writerWav.getOffsetRiffLength());
                writeInt(randomAccessFile, size - 8);
                randomAccessFile.seek(this.writerWav.getOffsetDataLength());
                writeInt(randomAccessFile, size - this.writerWav.getOffsetDataLength() - 4);
            }
        }
    }

    private final OutputStream stream;

    private long counter = 0L;

    private boolean dataHeaderExpected = true;
    private boolean headerExpected = true;

    private int offsetRiffLength;
    private int offsetDataLength;

    public WriterWav(final OutputStream stream) {
        this.stream = Objects.requireNonNull(stream);
    }

    public int getOffsetRiffLength() {
        return this.offsetRiffLength;
    }

    public int getOffsetDataLength() {
        return this.offsetDataLength;
    }

    private WriterWav writeInt(final int value) throws IOException {
        this.write(value);
        return this.write(value >> 8)
                .write(value >> 16)
                .write(value >> 24);
    }

    private WriterWav writeShort(final int value) throws IOException {
        return this.write(value).write(value >> 8);
    }

    private WriterWav write(final String text) throws IOException {
        for (int i = 0; i < text.length(); i++) {
            this.write(text.charAt(i));
        }
        return this;
    }

    private WriterWav write(final int value) throws IOException {
        this.stream.write(value);
        this.counter++;
        return this;
    }

    public WriterWav header(
            final int audioFormat,
            final int numChannels,
            final int sampleRate,
            final int byteRate,
            final int blockAlign,
            final int bitsPerSample,
            final int... extraParams
    ) throws IOException {
        if (this.headerExpected) {
            this.headerExpected = false;
        } else {
            throw new IllegalStateException("Header already written");
        }

        this.write("RIFF")
                .writeInt(0xFFFFFFFF)
                .write("WAVE");

        this.offsetRiffLength = (int) (this.counter - 8L);

        this.write("fmt ")
                .writeInt(16 + (extraParams.length == 0 ? 0 : 2 + extraParams.length))
                .writeShort(audioFormat)
                .writeShort(numChannels)
                .writeInt(sampleRate)
                .writeInt(byteRate)
                .writeShort(blockAlign)
                .writeShort(bitsPerSample);

        if (extraParams.length != 0) {
            this.writeShort(extraParams.length);
            for (final int v : extraParams) {
                this.write(v);
            }
        }

        return this;
    }

    private WriterWav ensureDataHeader() throws IOException {
        if (this.dataHeaderExpected) {
            this.dataHeaderExpected = false;
            this.write("data ")
                    .write(0xFFFFFFFF);
            this.offsetDataLength = (int) (this.counter - 4L);
        }
        return this;
    }

    public WriterWav data(final int data) throws IOException {
        this.ensureDataHeader()
                .write(data);
        return this;
    }

    public WriterWav data(final byte[] data) throws IOException {
        return this.data(data, 0, data.length);
    }

    public WriterWav data(final byte[] data, int offset, int length) throws IOException {
        this.ensureDataHeader();
        while (length > 0) {
            this.write(data[offset++]);
            length--;
        }
        return this;
    }

    public long getLength() {
        return this.counter;
    }

}
