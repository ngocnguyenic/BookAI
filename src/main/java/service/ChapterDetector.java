
package service;

import model.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterDetector {

    private static final String CHAPTER_PATTERN =
        "(?m)^\\s*(Chương|Bài|PHẦN|Chap|Chapter|Section|Part|B[àa]i)\\s+([\\dIVX]+)[\\s\\.:\\-–—]+.*$";

    public List<Chapter> detectChapters(String fullText) {
        List<Chapter> chapters = new ArrayList<>();
        Pattern pattern = Pattern.compile(CHAPTER_PATTERN);
        Matcher matcher = pattern.matcher(fullText);

        List<MatchResult> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }

        if (matches.isEmpty()) {
            // Nếu không tìm thấy → coi cả sách là 1 chương
            Chapter chap = new Chapter();
            chap.setChapterNumber(1);
            chap.setTitle("Toàn bộ nội dung");
            chap.setContent(fullText.trim());
            chapters.add(chap);
            return chapters;
        }

        for (int i = 0; i < matches.size(); i++) {
            MatchResult match = matches.get(i);
            int start = match.start();
            int end = (i == matches.size() - 1) ? fullText.length() : matches.get(i + 1).start();

            String title = match.group().trim();
            String content = fullText.substring(start, end).trim();

            Chapter chap = new Chapter();
            chap.setChapterNumber(i + 1);
            chap.setTitle(title);
            chap.setContent(content);
            chapters.add(chap);
        }

        return chapters;
    }
}