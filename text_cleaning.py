import re
import sys

def looks_like_json(text: str) -> bool:
    text = text.strip()
    # 1. Exception: Check for Transcript Timestamps
    if re.match(r"^\[\d{1,2}:\d{2}(:\d{2})?\]", text):
        return False
    # 2. Check for {
    if text.startswith("{"):
        return True
    # 3. Check for JSON Array
    if text.startswith("["):
        inner_content = text[1:].strip()
        if inner_content.startswith('"') or inner_content.startswith('{'):
            return True
    return False

def normalize_arabic(text: str) -> str:
    # Remove diacritics (tashkeel)
    text = re.sub(r'[\u064B-\u065F\u0670]', '', text)

    text = re.sub(r'[أإآ]', 'ا', text)
    text = re.sub(r'ة', 'ه', text)
    text = re.sub(r'ى', 'ي', text)
    
    return text

def clean_mixed_meeting_text(text):
    # Reject JSON
    if looks_like_json(text):
        return "JSON input is not supported. Please provide plain text transcript."

    # Remove unsupported characters (keep only Arabic, English, digits, punctuation, spaces)
    text = re.sub(r"[^a-zA-Z0-9\u0600-\u06FF\s.,!?؟]", "", text)

    # 1. Noise removal (timestamps, brackets)
    noise_pattern = r"\[\d{1,2}(?::\d{2}){1,2}\]|[\[\(].*?[\]\)]"
    text = re.sub(noise_pattern, "", text)

    # 2. Normalize Arabic Text
    text = normalize_arabic(text)

    # 3. Fillers
    english_fillers = {"um", "uh", "hmm", "erm", "ah", "er", "you know", "i mean", "kind of", "sort of"}
    
    arabic_fillers = {
        "يعني", "اصلا", "بصراحة", "بصراحه", "زي ما تقول", "هيك",
        "عرفت كيف", "بمعنى اصح", "خليني اقول", "ممكن نقول",
        "شو اسمه", "ايه ده", "بص", "اهو",
        "ااا", "اييه", 
        "بصي", "حضرتك", "يا جماعة", "معلش", "خلاص", "والله"
    }
    
    normalized_arabic_fillers = {normalize_arabic(f) for f in arabic_fillers}
    all_fillers = sorted(list(english_fillers) + list(normalized_arabic_fillers), key=len, reverse=True)
    filler_pattern = r"(?<![\w\u0621-\u064A])(" + "|".join(map(re.escape, all_fillers)) + r")(?![\w\u0621-\u064A])"
    text = re.sub(filler_pattern, "", text, flags=re.IGNORECASE)

    # 4. Remove stutter and phrase duplication
    text = re.sub(r'\b([\w\u0621-\u064A]+(?:\s+[\w\u0621-\u064A]+){0,4})(?:\s*[.,?!؟]*\s+\1\b)+', r'\1', text, flags=re.IGNORECASE)
    
    # 5. Cleanup whitespace & punctuation
    text = re.sub(r'[ \t]+', ' ', text)
    text = re.sub(r'\n+', '\n', text)
    text = re.sub(r'\.(\s*\.)+', '. ', text)
    text = re.sub(r'\.(\s*)([!?؟])', r'\1\2', text)
    text = re.sub(r'\.(\s*)([,،])', r'\1\2', text)
    text = re.sub(r'[,،](\s*[,،])+', '، ', text)
    text = re.sub(r'[ \t]+', ' ', text)

    return text.strip()


def main():
    # If arguments exist, use them
    if len(sys.argv) > 1:
        input_text = " ".join(sys.argv[1:])

    # If piped input exists
    elif not sys.stdin.isatty():
        input_text = sys.stdin.read()

    else:
        print("No input text provided.")
        sys.exit(1)

    cleaned = clean_mixed_meeting_text(input_text)
    print(cleaned)

if __name__ == "__main__":
    main()