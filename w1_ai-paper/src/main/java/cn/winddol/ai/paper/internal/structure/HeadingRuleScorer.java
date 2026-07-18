package cn.winddol.ai.paper.internal.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

class HeadingRuleScorer {

    List<HeadingDecision> score(List<HeadingCandidate> candidates) {
        List<HeadingDecision> decisions = new ArrayList<>();
        boolean titleSeen = false;
        boolean abstractSeen = false;
        boolean inReferences = false;
        boolean skippingNoiseBlock = false;
        boolean inAppendix = false;
        String titleKey = "";
        int lastHeadingLevel = 0;
        int lastMainSectionNumber = 0;

        for (HeadingCandidate candidate : candidates) {
            if (skippingNoiseBlock && !isPlausibleRestart(candidate, titleSeen)) {
                decisions.add(drop(candidate, CandidateKind.NOISE, 0.98, "inside publisher recommendation/noise block"));
                continue;
            }
            if (skippingNoiseBlock) {
                skippingNoiseBlock = false;
            }

            HeadingDecision decision;
            if (candidate.text().isBlank()) {
                decision = titleSeen ? text(candidate, CandidateKind.BODY, candidate.rawLine(), 1.0, "blank line preserved")
                        : drop(candidate, CandidateKind.BODY, 1.0, "leading blank before title");
            } else if (candidate.kind() == CandidateKind.NOISE) {
                if (startsNoiseBlock(candidate.text())) {
                    skippingNoiseBlock = true;
                }
                decision = drop(candidate, CandidateKind.NOISE, 0.99, "publisher/front-matter noise");
            } else if (!titleSeen) {
                decision = decideBeforeTitle(candidate);
                if (decision.action() == DecisionAction.EMIT_HEADING && decision.kind() == CandidateKind.TITLE) {
                    titleSeen = true;
                    titleKey = HeadingCandidateExtractor.key(decision.outputHeading());
                    lastHeadingLevel = 1;
                }
            } else if (isDuplicateTitle(candidate, titleKey)) {
                decision = drop(candidate, CandidateKind.NOISE, 0.95, "duplicate title/front-matter repeat");
            } else if (!abstractSeen && isFrontMatterHeadingBeforeAbstract(candidate)) {
                decision = drop(candidate, CandidateKind.NOISE, 0.88, "uncertain front-matter heading before abstract");
            } else if (inReferences && !HeadingCandidateExtractor.isReferences(candidate.text())) {
                decision = text(candidate, CandidateKind.REFERENCE_ITEM, candidateText(candidate), 0.98,
                        "line appears after References; keep as reference text");
            } else {
                decision = decideAfterTitle(candidate, lastHeadingLevel, lastMainSectionNumber, inAppendix);
                if (decision.action() == DecisionAction.EMIT_HEADING
                        || decision.action() == DecisionAction.SPLIT_HEADING_BODY) {
                    lastHeadingLevel = decision.targetLevel();
                    if (decision.kind() == CandidateKind.REFERENCES) {
                        inReferences = true;
                        inAppendix = false;
                    } else if (decision.kind() == CandidateKind.ABSTRACT) {
                        abstractSeen = true;
                    } else if (decision.kind() == CandidateKind.APPENDIX) {
                        inAppendix = true;
                    } else if (decision.targetLevel() <= 2) {
                        inAppendix = false;
                    }
                    Integer mainSectionNumber = topLevelNumber(decision.outputHeading());
                    if (decision.targetLevel() == 2 && mainSectionNumber != null) {
                        lastMainSectionNumber = mainSectionNumber;
                    }
                }
            }
            decisions.add(decision);
        }

        return decisions;
    }

    private HeadingDecision decideBeforeTitle(HeadingCandidate candidate) {
        if (HeadingCandidateExtractor.isFrontMatterNoise(candidate.text())) {
            return drop(candidate, CandidateKind.NOISE, 0.98, "front matter before title");
        }
        if (candidate.markdownHeading() && isPlausibleTitle(candidate.text())) {
            return heading(candidate, CandidateKind.TITLE, 1, HeadingCandidateExtractor.cleanTitle(candidate.text()),
                    0.98, "first plausible markdown heading is paper title");
        }
        if (!candidate.markdownHeading() && isPlausibleBareTitle(candidate.text())) {
            return heading(candidate, CandidateKind.TITLE, 1, HeadingCandidateExtractor.cleanTitle(candidate.text()),
                    0.82, "first plausible bare line is paper title");
        }
        return drop(candidate, CandidateKind.NOISE, 0.75, "non-title front matter before paper title");
    }

    private HeadingDecision decideAfterTitle(HeadingCandidate candidate,
                                             int lastHeadingLevel,
                                             int lastMainSectionNumber,
                                             boolean inAppendix) {
        if (candidate.kind() == CandidateKind.THEOREM_LIKE) {
            return text(candidate, CandidateKind.THEOREM_LIKE, candidateText(candidate), 0.95,
                    "theorem/lemma/figure/table-like text stays in body");
        }
        if (candidate.kind() == CandidateKind.LIST_ITEM) {
            return text(candidate, CandidateKind.LIST_ITEM, candidate.rawLine(), 0.95,
                    "numbered algorithm/list step stays in body");
        }
        if (candidate.kind() == CandidateKind.REFERENCE_ITEM) {
            return text(candidate, CandidateKind.REFERENCE_ITEM, candidate.rawLine(), 0.95,
                    "reference item stays in body");
        }
        if (candidate.kind() == CandidateKind.ABSTRACT) {
            return abstractDecision(candidate);
        }
        if (candidate.kind() == CandidateKind.REFERENCES) {
            return heading(candidate, CandidateKind.REFERENCES, 2, "References", 0.99,
                    "standard references heading");
        }
        if (candidate.kind() == CandidateKind.APPENDIX) {
            return appendixDecision(candidate);
        }
        if (candidate.kind() == CandidateKind.SECTION && isStandardInlineSection(candidate.text())) {
            return standardInlineSectionDecision(candidate);
        }
        if (candidate.markdownHeading()) {
            return explicitHeadingDecision(candidate, lastHeadingLevel, lastMainSectionNumber, inAppendix);
        }
        return bareLineDecision(candidate, lastMainSectionNumber, inAppendix);
    }

    private HeadingDecision abstractDecision(HeadingCandidate candidate) {
        String remainder = candidate.remainder();
        if (remainder != null && !remainder.isBlank()) {
            return split(candidate, CandidateKind.ABSTRACT, 2, "Abstract", remainder, 0.99,
                    "inline abstract split into heading and body");
        }
        return heading(candidate, CandidateKind.ABSTRACT, 2, "Abstract", 0.99, "standard abstract heading");
    }

    private HeadingDecision explicitHeadingDecision(HeadingCandidate candidate,
                                                    int lastHeadingLevel,
                                                    int lastMainSectionNumber,
                                                    boolean inAppendix) {
        int level = targetLevel(candidate, true, lastHeadingLevel, lastMainSectionNumber, inAppendix);
        CandidateKind kind = normalizeKind(candidate.kind(), candidate.text(), level);
        HeadingCandidateExtractor.InlineSplit inlineSplit = splitNumberedOrDecimalHeadingBody(candidate.text());
        if (!inlineSplit.remainder().isBlank()) {
            return split(candidate, kind, level, inlineSplit.heading(), inlineSplit.remainder(), 0.9,
                    "explicit markdown heading accepted and inline body split");
        }
        return heading(candidate, kind, level, normalizeSectionTitle(inlineSplit.heading()), 0.9,
                "explicit markdown heading accepted unless strong negative evidence exists");
    }

    private HeadingDecision bareLineDecision(HeadingCandidate candidate,
                                             int lastMainSectionNumber,
                                             boolean inAppendix) {
        if (candidate.kind() == CandidateKind.SUBSECTION) {
            Matcher matcher = HeadingCandidateExtractor.DECIMAL_SECTION.matcher(candidate.text());
            if (matcher.matches()) {
                HeadingCandidateExtractor.InlineSplit split = HeadingCandidateExtractor.splitInlineHeadingBody(matcher.group(2));
                String heading = matcher.group(1) + ". " + split.heading();
                String remainder = split.remainder();
                if (!remainder.isBlank()) {
                    return split(candidate, CandidateKind.SUBSECTION, 3, heading, remainder, 0.86,
                            "bare decimal subsection with inline body split");
                }
                return heading(candidate, CandidateKind.SUBSECTION, 3, heading, 0.86,
                        "bare decimal subsection accepted");
            }
        }
        if (candidate.kind() == CandidateKind.SECTION && isConfidentBareSection(candidate.text())) {
            int level = targetLevel(candidate, false, 0, lastMainSectionNumber, inAppendix);
            HeadingCandidateExtractor.InlineSplit inlineSplit = splitNumberedOrDecimalHeadingBody(candidate.text());
            if (!inlineSplit.remainder().isBlank()) {
                return split(candidate, CandidateKind.SECTION, level, inlineSplit.heading(), inlineSplit.remainder(), 0.84,
                        "bare line looks like a high-confidence section heading and inline body split");
            }
            return heading(candidate, CandidateKind.SECTION, level, normalizeSectionTitle(candidate.text()), 0.84,
                    "bare line looks like a high-confidence section heading");
        }
        if (candidate.kind() == CandidateKind.ABSTRACT || candidate.kind() == CandidateKind.REFERENCES) {
            return decideAfterTitle(candidate, 0, lastMainSectionNumber, inAppendix);
        }
        return text(candidate, CandidateKind.UNCERTAIN, candidate.rawLine(), 0.65,
                "low-confidence bare candidate preserved as body");
    }

    private int targetLevel(HeadingCandidate candidate,
                            boolean explicit,
                            int lastHeadingLevel,
                            int lastMainSectionNumber,
                            boolean inAppendix) {
        String text = candidate.text();
        if (candidate.kind() == CandidateKind.ABSTRACT || candidate.kind() == CandidateKind.REFERENCES
                || HeadingCandidateExtractor.isStandardSection(text)) {
            return 2;
        }
        if (HeadingCandidateExtractor.CHAPTER_SECTION.matcher(text).matches()) {
            return 2;
        }
        if (HeadingCandidateExtractor.APPENDIX_SECTION.matcher(text).matches()) {
            return 2;
        }
        Matcher decimal = HeadingCandidateExtractor.DECIMAL_SECTION.matcher(text);
        if (decimal.matches()) {
            int depth = decimal.group(1).split("\\.").length;
            return Math.min(depth + 1, 4);
        }
        if (HeadingCandidateExtractor.SECTION_SIGN_NUMBERED.matcher(text).matches()) {
            return 2;
        }
        Matcher letter = HeadingCandidateExtractor.LETTER_SECTION.matcher(text);
        if (letter.matches() && isLetterSubsectionMarker(letter.group(1))) {
            return 3;
        }
        if (HeadingCandidateExtractor.ROMAN_SECTION.matcher(text).matches()) {
            return 2;
        }
        if (letter.matches()) {
            return 3;
        }
        Matcher numbered = HeadingCandidateExtractor.NUMBERED_SECTION.matcher(text);
        if (numbered.matches()) {
            int number = Integer.parseInt(numbered.group(1));
            if (inAppendix) {
                return 3;
            }
            if (explicit && lastMainSectionNumber > 0 && number > lastMainSectionNumber) {
                return 2;
            }
            if (explicit && lastHeadingLevel >= 3 && !isMajorSectionText(numbered.group(2))) {
                return 4;
            }
            return 2;
        }
        if (explicit) {
            if (inAppendix) {
                return 3;
            }
            if (lastHeadingLevel >= 3 && isLocalSubheadingText(text)) {
                return Math.min(lastHeadingLevel + 1, 4);
            }
            return candidate.sourceLevel() <= 1 ? 2 : Math.min(candidate.sourceLevel(), 4);
        }
        return 0;
    }

    private CandidateKind normalizeKind(CandidateKind kind, String text, int targetLevel) {
        if (targetLevel >= 3
                && (HeadingCandidateExtractor.LETTER_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.DECIMAL_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.NUMBERED_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.SECTION_SIGN_NUMBERED.matcher(text).matches())) {
            return CandidateKind.SUBSECTION;
        }
        if (kind != CandidateKind.UNCERTAIN) {
            return kind;
        }
        if (HeadingCandidateExtractor.NUMBERED_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.SECTION_SIGN_NUMBERED.matcher(text).matches()
                || HeadingCandidateExtractor.ROMAN_SECTION.matcher(text).matches()) {
            return CandidateKind.SECTION;
        }
        if (HeadingCandidateExtractor.LETTER_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.DECIMAL_SECTION.matcher(text).matches()) {
            return CandidateKind.SUBSECTION;
        }
        return CandidateKind.SECTION;
    }

    private boolean isLetterSubsectionMarker(String marker) {
        return marker != null
                && marker.length() == 1
                && !"IVX".contains(marker);
    }

    private boolean isPlausibleRestart(HeadingCandidate candidate, boolean titleSeen) {
        if (candidate.text().isBlank()) {
            return false;
        }
        if (candidate.kind() == CandidateKind.NOISE || candidate.kind() == CandidateKind.THEOREM_LIKE) {
            return false;
        }
        return candidate.markdownHeading()
                && (HeadingCandidateExtractor.isStandardSection(candidate.text())
                || candidate.kind() == CandidateKind.ABSTRACT
                || candidate.kind() == CandidateKind.REFERENCES
                || candidate.kind() == CandidateKind.SECTION
                || candidate.kind() == CandidateKind.SUBSECTION
                || candidate.kind() == CandidateKind.APPENDIX
                || (titleSeen && isPlausibleTitle(candidate.text()))
                || (!titleSeen && isPlausibleTitle(candidate.text())));
    }

    private boolean startsNoiseBlock(String text) {
        String key = HeadingCandidateExtractor.key(text);
        return key.equals("paper")
                || key.equals("affiliations")
                || key.equals("contents")
                || key.equals("aip advances")
                || key.equals("why publish with us")
                || key.equals("you may also like")
                || key.equals("articles you may be interested in");
    }

    private boolean isDuplicateTitle(HeadingCandidate candidate, String titleKey) {
        if (titleKey == null || titleKey.isBlank()) {
            return false;
        }
        return candidate.markdownHeading()
                && titleKey.equals(HeadingCandidateExtractor.key(HeadingCandidateExtractor.cleanTitle(candidate.text())));
    }

    private boolean isFrontMatterHeadingBeforeAbstract(HeadingCandidate candidate) {
        if (!candidate.markdownHeading()) {
            return false;
        }
        if (candidate.kind() != CandidateKind.UNCERTAIN) {
            return false;
        }
        return !HeadingCandidateExtractor.CHAPTER_SECTION.matcher(candidate.text()).matches()
                && !HeadingCandidateExtractor.NUMBERED_SECTION.matcher(candidate.text()).matches()
                && !HeadingCandidateExtractor.ROMAN_SECTION.matcher(candidate.text()).matches();
    }

    private boolean isPlausibleTitle(String text) {
        String cleaned = HeadingCandidateExtractor.cleanTitle(text);
        String key = HeadingCandidateExtractor.key(cleaned);
        return cleaned.length() >= 8
                && !HeadingCandidateExtractor.isNoiseHeading(cleaned)
                && !HeadingCandidateExtractor.isFrontMatterNoise(cleaned)
                && !HeadingCandidateExtractor.isStandardSection(cleaned)
                && !HeadingCandidateExtractor.NUMBERED_SECTION.matcher(cleaned).matches()
                && !HeadingCandidateExtractor.ROMAN_SECTION.matcher(cleaned).matches()
                && !HeadingCandidateExtractor.LETTER_SECTION.matcher(cleaned).matches()
                && !key.equals("index");
    }

    private boolean isPlausibleBareTitle(String text) {
        if (!isPlausibleTitle(text)) {
            return false;
        }
        String cleaned = HeadingCandidateExtractor.cleanTitle(text);
        int latinWords = 0;
        for (String word : cleaned.split("\\s+")) {
            if (word.matches(".*[A-Za-z]{2,}.*")) {
                latinWords++;
            }
        }
        return latinWords >= 3;
    }

    private boolean isConfidentBareSection(String text) {
        Matcher numbered = HeadingCandidateExtractor.NUMBERED_SECTION.matcher(text);
        if (numbered.matches()) {
            return isMajorSectionText(numbered.group(2));
        }
        return HeadingCandidateExtractor.ROMAN_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.LETTER_SECTION.matcher(text).matches()
                || HeadingCandidateExtractor.isStandardSection(text);
    }

    private HeadingDecision appendixDecision(HeadingCandidate candidate) {
        Matcher matcher = HeadingCandidateExtractor.APPENDIX_SECTION.matcher(candidate.text());
        if (matcher.matches()) {
            HeadingCandidateExtractor.InlineSplit split = HeadingCandidateExtractor.splitInlineHeadingBody(matcher.group(2));
            String heading = matcher.group(1) + ". " + split.heading();
            if (!split.remainder().isBlank()) {
                return split(candidate, CandidateKind.APPENDIX, 2, normalizeSectionTitle(heading), split.remainder(), 0.92,
                        "appendix heading with inline body split");
            }
            return heading(candidate, CandidateKind.APPENDIX, 2, normalizeSectionTitle(heading), 0.92,
                    "appendix heading");
        }
        return heading(candidate, CandidateKind.APPENDIX, 2, normalizeSectionTitle(candidate.text()), 0.92,
                "appendix heading");
    }

    private HeadingDecision standardInlineSectionDecision(HeadingCandidate candidate) {
        String heading = standardInlineHeading(candidate.text());
        String remainder = standardInlineRemainder(candidate.text(), heading);
        if (!remainder.isBlank()) {
            return split(candidate, CandidateKind.SECTION, 2, heading, remainder, 0.9,
                    "standard section heading with inline body split");
        }
        return heading(candidate, CandidateKind.SECTION, 2, heading, 0.9,
                "standard section heading");
    }

    private boolean isStandardInlineSection(String text) {
        String lower = HeadingCandidateExtractor.key(text);
        return lower.startsWith("acknowledgements")
                || lower.startsWith("acknowledgments")
                || lower.startsWith("data availability");
    }

    private String standardInlineHeading(String text) {
        String lower = HeadingCandidateExtractor.key(text);
        if (lower.startsWith("acknowledgements")) {
            return "Acknowledgements";
        }
        if (lower.startsWith("acknowledgments")) {
            return "Acknowledgments";
        }
        if (lower.startsWith("data availability")) {
            return "Data Availability";
        }
        return normalizeSectionTitle(text);
    }

    private String standardInlineRemainder(String text, String heading) {
        String cleaned = HeadingCandidateExtractor.clean(text);
        if (cleaned.length() <= heading.length()) {
            return "";
        }
        String remainder = cleaned.substring(heading.length()).trim();
        return remainder.replaceFirst("^[:.]\\s*", "").trim();
    }

    private HeadingCandidateExtractor.InlineSplit splitNumberedOrDecimalHeadingBody(String text) {
        Matcher decimal = HeadingCandidateExtractor.DECIMAL_SECTION.matcher(text);
        if (decimal.matches()) {
            HeadingCandidateExtractor.InlineSplit split = HeadingCandidateExtractor.splitInlineHeadingBody(decimal.group(2));
            return new HeadingCandidateExtractor.InlineSplit(decimal.group(1) + ". " + split.heading(), split.remainder());
        }
        Matcher numbered = HeadingCandidateExtractor.NUMBERED_SECTION.matcher(text);
        if (numbered.matches()) {
            HeadingCandidateExtractor.InlineSplit split = HeadingCandidateExtractor.splitInlineHeadingBody(numbered.group(2));
            return new HeadingCandidateExtractor.InlineSplit(numbered.group(1) + ". " + split.heading(), split.remainder());
        }
        Matcher sectionSign = HeadingCandidateExtractor.SECTION_SIGN_NUMBERED.matcher(text);
        if (sectionSign.matches()) {
            HeadingCandidateExtractor.InlineSplit split = HeadingCandidateExtractor.splitInlineHeadingBody(sectionSign.group(2));
            return new HeadingCandidateExtractor.InlineSplit(sectionSign.group(1) + ". " + split.heading(), split.remainder());
        }
        return new HeadingCandidateExtractor.InlineSplit(text, "");
    }

    private Integer topLevelNumber(String text) {
        Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)(?:\\.|\\s)\\s+.+$").matcher(text);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private boolean isLocalSubheadingText(String text) {
        String lower = HeadingCandidateExtractor.key(text);
        return lower.equals("the algorithm")
                || lower.matches("method\\s+\\d+\\.?")
                || lower.equals("algorithm");
    }

    private boolean isMajorSectionText(String text) {
        String cleaned = HeadingCandidateExtractor.clean(text);
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (HeadingCandidateExtractor.isLikelyListItem(cleaned)) {
            return false;
        }
        return isMostlyUppercase(cleaned)
                || lower.startsWith("introduction")
                || lower.startsWith("background")
                || lower.startsWith("method")
                || lower.startsWith("model")
                || lower.startsWith("experiment")
                || lower.startsWith("result")
                || lower.startsWith("discussion")
                || lower.startsWith("conclusion")
                || lower.startsWith("summary");
    }

    private boolean isMostlyUppercase(String text) {
        int letters = 0;
        int uppercase = 0;
        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch)) {
                letters++;
                if (Character.isUpperCase(ch)) {
                    uppercase++;
                }
            }
        }
        return letters > 0 && uppercase >= Math.max(3, (int) Math.ceil(letters * 0.65));
    }

    private String normalizeSectionTitle(String text) {
        String cleaned = HeadingCandidateExtractor.clean(text);
        if (cleaned.equalsIgnoreCase("ABSTRACT")) {
            return "Abstract";
        }
        if (cleaned.equalsIgnoreCase("REFERENCES")) {
            return "References";
        }
        return cleaned;
    }

    private String candidateText(HeadingCandidate candidate) {
        return candidate.markdownHeading() ? candidate.text() : candidate.rawLine();
    }

    private HeadingDecision heading(HeadingCandidate candidate,
                                    CandidateKind kind,
                                    int targetLevel,
                                    String outputHeading,
                                    double confidence,
                                    String reason) {
        return new HeadingDecision(candidate, kind, DecisionAction.EMIT_HEADING, targetLevel, confidence,
                outputHeading, "", List.of(reason));
    }

    private HeadingDecision split(HeadingCandidate candidate,
                                  CandidateKind kind,
                                  int targetLevel,
                                  String outputHeading,
                                  String outputText,
                                  double confidence,
                                  String reason) {
        return new HeadingDecision(candidate, kind, DecisionAction.SPLIT_HEADING_BODY, targetLevel, confidence,
                outputHeading, outputText, List.of(reason));
    }

    private HeadingDecision text(HeadingCandidate candidate,
                                 CandidateKind kind,
                                 String outputText,
                                 double confidence,
                                 String reason) {
        return new HeadingDecision(candidate, kind, DecisionAction.EMIT_TEXT, 0, confidence,
                "", outputText, List.of(reason));
    }

    private HeadingDecision drop(HeadingCandidate candidate,
                                 CandidateKind kind,
                                 double confidence,
                                 String reason) {
        return new HeadingDecision(candidate, kind, DecisionAction.DROP, 0, confidence,
                "", "", List.of(reason));
    }
}
