package com.phoneunison.desktop.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BasicFileUploadHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = LoggerFactory.getLogger(BasicFileUploadHandler.class);
    private static final String UPLOAD_URI = "/upload";
    private final String uploadDir;

    public BasicFileUploadHandler(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    private boolean isUploading = false;
    private FileOutputStream fileOutputStream;
    private File currentFile;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            if (request.uri().startsWith(UPLOAD_URI) && request.method() == HttpMethod.POST) {
                isUploading = true;
                String fileName = getFileName(request);
                startUpload(fileName);
                return;
            }
        }

        if (isUploading) {
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();
                try {
                    if (buf.isReadable()) {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        if (fileOutputStream != null) {
                            fileOutputStream.write(bytes);
                        }
                    }

                    if (msg instanceof LastHttpContent) {
                        finishUpload(ctx);
                    }
                } catch (IOException e) {
                    logger.error("Error writing file chunk", e);
                    cleanupUpload();
                    sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error writing file");
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private String getFileName(HttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (decoder.parameters().containsKey("filename")) {
            return decoder.parameters().get("filename").get(0);
        }
        return "received_file_" + System.currentTimeMillis();
    }

    private void startUpload(String fileName) {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        currentFile = new File(dir, fileName);
        try {
            fileOutputStream = new FileOutputStream(currentFile);
            logger.info("Starting upload: {}", currentFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to open file for writing", e);
        }
    }

    private void finishUpload(ChannelHandlerContext ctx) {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            logger.info("Upload completed: {}", currentFile.getAbsolutePath());
            sendResponse(ctx, HttpResponseStatus.OK, "Upload complete");
        } catch (IOException e) {
            logger.error("Failed to close file", e);
            sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error saving file");
        } finally {
            isUploading = false;
            fileOutputStream = null;
        }
    }

    private void cleanupUpload() {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException ignored) {
        } finally {
            isUploading = false;
            fileOutputStream = null;
            if (currentFile != null && currentFile.exists()) {
                currentFile.delete();
            }
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status);
        response.content().writeBytes(message.getBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Upload error", cause);
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException ignored) {
        }
        ctx.close();
    }
}
