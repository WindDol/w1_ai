# -- coding: utf-8 --
import nest_asyncio
import os
import sys
import json
from llama_parse import LlamaParse

nest_asyncio.apply()

# 建议将 API KEY 设为环境变量，或者由 Java 传入
API_KEY = os.getenv("LLAMA_PARSE_API_KEY", "llx-TKyeAEOnyR1csKmSeXXjZZgKboZ54jLagQVWPU8GJP2llj7Z")
#llx-TKyeAEOnyR1csKmSeXXjZZgKboZ54jLagQVWPU8GJP2llj7Z
def parse_pdf(pdf_path):
    instruction = """
    You are a precision content extractor. Your absolute priority is creating clean, continuous Markdown from a PDF that contains severe layout interruptions.

    ### 1. THE "KILL LIST" (STRICT REMOVAL)
    You must scan for and DELETE specific layout artifacts that appear between text blocks.
    **Specific Patterns to DELETE immediately:**
    - **Journal Metadata**: Lines containing "J. Phys. A: Math. Theor", "Vol", "2024", or similar journal codes.
    - **Author Headers**: Lines ending in "et al" (e.g., "A Crnkić et al") appearing at the top/bottom of pages.
    - **Page Numbers**: Isolated numbers (e.g., "3", "4") that appear between paragraphs.
    - **Copyright/DOI**: Any line starting with "©", "DOI:", or "Downloaded from".

    ### 2. SENTENCE REPAIR (CRITICAL)
    The document text is broken by these artifacts. You must reconstruct the flow.
    - **Rule**: If a sentence does not end with punctuation (., ?, !), and is followed by [Noise], IGNORE the noise and connect it to the next text block.
    - **Example Scenario**:
        - *Raw Input*: "...the equation depends on [3 J. Phys. A... et al] the variable x."
        - *Your Output*: "...the equation depends on the variable x."

    ### 3. DOCUMENT STRUCTURE
    - **# Title**: Use only for the main paper title.
    - **## Section**: Use for "Abstract", "Introduction", "Model", "Acknowledgments", "References".
    - **### Sub-section**: Use for "2.1 Derivation", "3.1 Setup".
    - **Heading Logic**: "References" and "Acknowledgments" are ALWAYS `##`. Do not let font size fool you into making them `#`.

    ### 4. MATH & CONTENT
    - Preserve LaTeX math ($...$).
    - Keep Table data.
    - **Output ONLY the cleaned Markdown.**
    """

    parser = LlamaParse(
        api_key=API_KEY,
        result_type="markdown",
        user_prompt=instruction,
        premium_mode=True,
        language="en",
        verbose=False
    )

    if not os.path.exists(pdf_path):
        return {"status": "error", "message": f"File not found: {pdf_path}"}

    try:
        documents = parser.load_data(pdf_path)
        full_text = "\n\n".join([doc.text for doc in documents])

        # 返回 JSON 格式，方便 Java 解析
        return {
            "status": "success",
            "content": full_text
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

if __name__ == "__main__":
    # 从命令行获取文件路径
    if len(sys.argv) < 2:
        print(json.dumps({"status": "error", "message": "No file path provided"}))
        sys.exit(1)

    target_pdf = sys.argv[1]
    result = parse_pdf(target_pdf)

    # 核心：将 JSON 打印到 stdout，Java 会读取这里
    print(json.dumps(result))