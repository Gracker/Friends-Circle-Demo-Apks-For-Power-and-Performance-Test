package com.example.wechatfriendforebook.parser;

import android.content.Context;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * EPUB 电子书解析器
 * EPUB 文件本质上是一个 ZIP 压缩包，包含 HTML/XHTML 内容文件
 */
public class EpubParser {
    private static final String TAG = "EpubParser";

    private Context context;
    private File extractDir;
    private String bookTitle = "未知书名";
    private String bookAuthor = "未知作者";
    private List<Chapter> chapters = new ArrayList<>();
    private List<String> allContent = new ArrayList<>();

    public EpubParser(Context context) {
        this.context = context;
        this.extractDir = new File(context.getCacheDir(), "epub_extract");
    }

    /**
     * 从 assets 目录解析 EPUB 文件
     */
    public boolean parseFromAssets(String assetFileName) {
        try {
            InputStream is = context.getAssets().open(assetFileName);
            return parseFromInputStream(is);
        } catch (IOException e) {
            Log.e(TAG, "无法从 assets 打开文件: " + assetFileName, e);
            return false;
        }
    }

    /**
     * 从输入流解析 EPUB 文件
     */
    public boolean parseFromInputStream(InputStream inputStream) {
        try {
            // 清理之前的解压目录
            if (extractDir.exists()) {
                deleteDirectory(extractDir);
            }
            extractDir.mkdirs();

            // 解压 EPUB 文件
            extractZip(inputStream);

            // 解析 container.xml 获取 content.opf 路径
            String opfPath = parseContainer();
            if (opfPath == null) {
                Log.e(TAG, "无法找到 content.opf 文件");
                return false;
            }

            // 解析 content.opf 获取书籍信息和章节列表
            parseOpf(opfPath);

            // 加载所有章节内容
            loadAllChapters(opfPath);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "解析 EPUB 失败", e);
            return false;
        }
    }

    /**
     * 解压 ZIP 文件
     */
    private void extractZip(InputStream inputStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry entry;
        byte[] buffer = new byte[4096];

        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(extractDir, entry.getName());

            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                // 确保父目录存在
                file.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * 解析 META-INF/container.xml 获取 content.opf 路径
     */
    private String parseContainer() {
        try {
            File containerFile = new File(extractDir, "META-INF/container.xml");
            if (!containerFile.exists()) {
                Log.e(TAG, "container.xml 不存在");
                return null;
            }

            Document doc = Jsoup.parse(containerFile, "UTF-8");
            Element rootfile = doc.selectFirst("rootfile");
            if (rootfile != null) {
                return rootfile.attr("full-path");
            }
        } catch (IOException e) {
            Log.e(TAG, "解析 container.xml 失败", e);
        }
        return null;
    }

    /**
     * 解析 content.opf 获取书籍元数据
     */
    private void parseOpf(String opfPath) {
        try {
            File opfFile = new File(extractDir, opfPath);
            Document doc = Jsoup.parse(opfFile, "UTF-8");

            // 获取书名
            Element titleElement = doc.selectFirst("dc\\:title, title");
            if (titleElement != null) {
                bookTitle = titleElement.text();
            }

            // 获取作者
            Element authorElement = doc.selectFirst("dc\\:creator, creator");
            if (authorElement != null) {
                bookAuthor = authorElement.text();
            }

            // 获取章节列表（从 spine 中按顺序获取）
            Elements spineItems = doc.select("spine itemref");
            Elements manifestItems = doc.select("manifest item");

            for (Element spineItem : spineItems) {
                String idref = spineItem.attr("idref");

                // 在 manifest 中找到对应的 item
                for (Element manifestItem : manifestItems) {
                    if (manifestItem.attr("id").equals(idref)) {
                        String href = manifestItem.attr("href");
                        String mediaType = manifestItem.attr("media-type");

                        // 只处理 HTML/XHTML 文件
                        if (mediaType.contains("html") || mediaType.contains("xml")) {
                            Chapter chapter = new Chapter();
                            chapter.href = href;
                            chapter.title = "章节 " + (chapters.size() + 1);
                            chapters.add(chapter);
                        }
                        break;
                    }
                }
            }

            Log.d(TAG, "解析完成: " + bookTitle + " by " + bookAuthor + ", " + chapters.size() + " 章节");

        } catch (IOException e) {
            Log.e(TAG, "解析 content.opf 失败", e);
        }
    }

    /**
     * 加载所有章节内容
     */
    private void loadAllChapters(String opfPath) {
        // 获取 OPF 文件所在目录，作为相对路径的基准
        String opfDir = "";
        int lastSlash = opfPath.lastIndexOf('/');
        if (lastSlash > 0) {
            opfDir = opfPath.substring(0, lastSlash + 1);
        }

        for (Chapter chapter : chapters) {
            try {
                String fullPath = opfDir + chapter.href;
                File chapterFile = new File(extractDir, fullPath);

                if (!chapterFile.exists()) {
                    // 尝试直接使用 href
                    chapterFile = new File(extractDir, chapter.href);
                }

                if (chapterFile.exists()) {
                    Document doc = Jsoup.parse(chapterFile, "UTF-8");

                    // 尝试获取章节标题
                    Element titleElement = doc.selectFirst("h1, h2, h3, title");
                    if (titleElement != null && !titleElement.text().trim().isEmpty()) {
                        chapter.title = titleElement.text().trim();
                    }

                    // 获取正文内容
                    Element body = doc.body();
                    if (body != null) {
                        String text = cleanHtmlContent(body);
                        chapter.content = text;

                        // 将内容分段添加到 allContent
                        String[] paragraphs = text.split("\n\n+");
                        for (String paragraph : paragraphs) {
                            String trimmed = paragraph.trim();
                            if (!trimmed.isEmpty()) {
                                allContent.add(trimmed);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "加载章节失败: " + chapter.href, e);
            }
        }

        Log.d(TAG, "加载完成，共 " + allContent.size() + " 个段落");
    }

    /**
     * 清理 HTML 内容，提取纯文本
     */
    private String cleanHtmlContent(Element element) {
        StringBuilder sb = new StringBuilder();

        // 处理段落、标题等块级元素
        Elements blocks = element.select("p, h1, h2, h3, h4, h5, h6, div, li");

        if (blocks.isEmpty()) {
            // 如果没有块级元素，直接获取文本
            return element.text();
        }

        for (Element block : blocks) {
            String text = block.text().trim();
            if (!text.isEmpty()) {
                sb.append(text).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    // Getters

    public String getBookTitle() {
        return bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public List<String> getAllContent() {
        return allContent;
    }

    public int getTotalParagraphs() {
        return allContent.size();
    }

    /**
     * 获取指定范围的段落
     */
    public String getContentRange(int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end && i < allContent.size(); i++) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(allContent.get(i));
        }
        return sb.toString();
    }

    /**
     * 章节信息类
     */
    public static class Chapter {
        public String href;
        public String title;
        public String content;

        @Override
        public String toString() {
            return title;
        }
    }
}

