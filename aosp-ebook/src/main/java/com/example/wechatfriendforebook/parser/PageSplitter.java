package com.example.wechatfriendforebook.parser;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面分割器
 * 将电子书内容按照屏幕大小分割成页面
 */
public class PageSplitter {
    private static final String TAG = "PageSplitter";

    private int pageWidth;
    private int pageHeight;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private float textSize;
    private float lineSpacing;
    private float paragraphSpacing;

    private TextPaint textPaint;
    private List<String> pages = new ArrayList<>();

    public PageSplitter(int pageWidth, int pageHeight) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;

        // 默认内边距
        this.paddingLeft = 48;
        this.paddingTop = 64;
        this.paddingRight = 48;
        this.paddingBottom = 80;

        // 默认文字大小和行距
        this.textSize = 48f;
        this.lineSpacing = 1.5f;
        this.paragraphSpacing = 32f;

        // 初始化画笔
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
    }

    /**
     * 设置内边距
     */
    public void setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
    }

    /**
     * 设置文字大小
     */
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        textPaint.setTextSize(textSize);
    }

    /**
     * 设置行距倍数
     */
    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    /**
     * 设置段落间距
     */
    public void setParagraphSpacing(float paragraphSpacing) {
        this.paragraphSpacing = paragraphSpacing;
    }

    /**
     * 分割内容为页面
     */
    public void splitPages(List<String> paragraphs) {
        pages.clear();

        if (paragraphs == null || paragraphs.isEmpty()) {
            return;
        }

        int contentWidth = pageWidth - paddingLeft - paddingRight;
        int contentHeight = pageHeight - paddingTop - paddingBottom;

        float lineHeight = textSize * lineSpacing;

        StringBuilder currentPage = new StringBuilder();
        float currentHeight = 0;

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);

            // 计算段落需要的行数和高度
            List<String> lines = wrapText(paragraph, contentWidth);
            float paragraphHeight = lines.size() * lineHeight;

            // 如果当前页面加上这个段落会超出，先保存当前页面
            if (currentHeight > 0 && currentHeight + paragraphHeight + paragraphSpacing > contentHeight) {
                // 保存当前页面
                if (currentPage.length() > 0) {
                    pages.add(currentPage.toString().trim());
                    currentPage = new StringBuilder();
                    currentHeight = 0;
                }
            }

            // 如果单个段落超过一页，需要分割
            if (paragraphHeight > contentHeight) {
                // 分割大段落
                splitLargeParagraph(lines, lineHeight, contentHeight, currentPage, currentHeight);
                currentPage = new StringBuilder();
                currentHeight = 0;
            } else {
                // 添加段落到当前页面
                if (currentPage.length() > 0) {
                    currentPage.append("\n\n");
                    currentHeight += paragraphSpacing;
                }
                currentPage.append(paragraph);
                currentHeight += paragraphHeight;
            }
        }

        // 保存最后一页
        if (currentPage.length() > 0) {
            pages.add(currentPage.toString().trim());
        }

        Log.d(TAG, "分页完成，共 " + pages.size() + " 页");
    }

    /**
     * 分割大段落（超过一页的内容）
     */
    private void splitLargeParagraph(List<String> lines, float lineHeight, int contentHeight,
                                     StringBuilder currentPage, float currentHeight) {
        int linesPerPage = (int) ((contentHeight - currentHeight) / lineHeight);

        StringBuilder pageContent = new StringBuilder();
        if (currentPage.length() > 0) {
            pageContent.append(currentPage.toString());
            if (!currentPage.toString().endsWith("\n\n")) {
                pageContent.append("\n\n");
            }
        }

        int lineCount = 0;
        for (String line : lines) {
            if (lineCount >= linesPerPage && pageContent.length() > 0) {
                // 保存当前页
                pages.add(pageContent.toString().trim());
                pageContent = new StringBuilder();
                lineCount = 0;
                linesPerPage = (int) (contentHeight / lineHeight);
            }

            pageContent.append(line);
            if (!line.endsWith("\n")) {
                pageContent.append("\n");
            }
            lineCount++;
        }

        // 处理剩余内容
        if (pageContent.length() > 0) {
            pages.add(pageContent.toString().trim());
        }
    }

    /**
     * 文本换行处理
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return lines;
        }

        // 首行缩进两个字符
        String indent = "　　";
        String currentLine = indent;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 处理换行符
            if (c == '\n') {
                if (currentLine.length() > 0) {
                    lines.add(currentLine);
                }
                currentLine = indent;
                continue;
            }

            String testLine = currentLine + c;
            float width = textPaint.measureText(testLine);

            if (width > maxWidth) {
                // 当前行已满，保存并开始新行
                if (currentLine.length() > 0) {
                    lines.add(currentLine);
                }
                currentLine = String.valueOf(c);
            } else {
                currentLine = testLine;
            }
        }

        // 添加最后一行
        if (currentLine.length() > 0) {
            lines.add(currentLine);
        }

        return lines;
    }

    /**
     * 获取所有页面
     */
    public List<String> getPages() {
        return pages;
    }

    /**
     * 获取总页数
     */
    public int getPageCount() {
        return pages.size();
    }

    /**
     * 获取指定页面的内容
     */
    public String getPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            return pages.get(pageIndex);
        }
        return "";
    }
}

