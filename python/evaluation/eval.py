import json
import os
import pandas as pd
from datasets import Dataset
from ragas import evaluate
from ragas.metrics import Faithfulness, AnswerRelevancy, ContextRecall, ContextPrecision
from pydantic import SecretStr
# 引入两个供应商的适配器
from langchain_openai import ChatOpenAI
from langchain_google_genai import GoogleGenerativeAIEmbeddings

from ragas.llms import LangchainLLMWrapper
from ragas.embeddings import LangchainEmbeddingsWrapper

# ==========================================
# 🌐 1. 网络代理配置 (根据你的 VPN 端口 10810)
# ==========================================
proxy_url = "http://127.0.0.1:10810"
os.environ["http_proxy"] = proxy_url
os.environ["https_proxy"] = proxy_url

# ==========================================
# ⚙️ 2. 配置 Google Gemini 作为裁判
# ==========================================
# 填入你的 Google API Key
os.environ["GOOGLE_API_KEY"] = "AIzaSyC2cywgWs5SPJBDnNx0JJc3etiZwYb7nyU"
os.environ["DEEPSEEK_API_KEY"] = "sk-4e6a9a95491a408b9b816c708c338d56"

# 配置 LLM (用于打分)
# 使用 1.5-flash 速度快且免费额度高
deepseek_model = ChatOpenAI(
    model="deepseek-chat",
    api_key=SecretStr(os.environ["DEEPSEEK_API_KEY"]),
    base_url="https://api.deepseek.com",
    temperature=0,
    max_retries=3
)

# 配置 Embedding (用于计算 AnswerRelevancy)
gemini_embeddings = GoogleGenerativeAIEmbeddings(
    model="models/gemini-embedding-001"
)

# 包装成 RAGAS 识别的对象
evaluator_llm = LangchainLLMWrapper(deepseek_model)
evaluator_embeddings = LangchainEmbeddingsWrapper(gemini_embeddings)
def clean_content(text):
    if "### 📄 Current Section Content" in text:
        parts = text.split("### 📄 Current Section Content")
        # 只取正文部分，后面的 Nearby Sections 也可以考虑去掉
        return parts[1].split("### 🧭 Nearby Sections")[0]
    return text

def load_and_process_data(file_path):
    print(f"📂 Loading data from {file_path}...")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"找不到文件: {file_path}")

    with open(file_path, 'r', encoding='utf-8') as f:
        raw_data = json.load(f)

    data_dict = {
        'user_input': [],         # 对应旧版 question
        'retrieved_contexts': [], # 对应旧版 contexts
        'response': [],           # 对应旧版 answer
        'reference': []           # 对应旧版 ground_truth
    }

    for item in raw_data:
        q = item.get('question') or item.get('user_input')
        a = item.get('answer') or item.get('response')
        gt = item.get('ground_truth') or item.get('reference') or ""
        raw_contexts = item.get('contents') or item.get('contexts') or []
        # 截取前 4000 字符，Gemini 窗口大
        cleaned_contexts = [str(c)[:30000] for c in raw_contexts]
        data_dict['user_input'].append(q)
        data_dict['response'].append(a)
        data_dict['retrieved_contexts'].append(cleaned_contexts)
        data_dict['reference'].append(gt)

    return Dataset.from_dict(data_dict)


def run_evaluation():
    # 确保 test_results.json 路径正确
    dataset = load_and_process_data("test_results.json")

    print("🤖 Starting RAGAS evaluation with Google Gemini via Proxy...")

    # 定义指标
    metrics = [
        Faithfulness(),
        AnswerRelevancy(),
        ContextRecall(),
        ContextPrecision()
    ]

    # 执行评估
    results = evaluate(
        dataset=dataset,
        metrics=metrics,
        llm=evaluator_llm,
        embeddings=evaluator_embeddings
    )

    print("\n📊 ====== Final Scores ======")
    print(results)

    # 导出报告
    df = results.to_pandas()
    df.to_csv("ragas_report_mixed.csv", index=False, encoding='utf-8-sig')
    print(f"\n✅ Report saved to ragas_report_mixed.csv")

if __name__ == "__main__":
    run_evaluation()