import streamlit as st
import requests
import json
import uuid
import html  # 引入 html 库用于转义
from sseclient import SSEClient
import textwrap
import pandas as pd
# --- 1. 页面基础配置 ---
st.set_page_config(
    page_title="ScholarBrain 1.0 - 深度科研",
    page_icon="🧠",
    layout="wide",
    initial_sidebar_state="expanded"
)

# --- 2. CSS 深度美化 (关键修复) ---
st.markdown("""
    <style>
    /* 全局字体 */
    .stApp { font-family: 'Inter', system-ui, sans-serif; }
    
    /* 聊天气泡 */
    .stChatMessage { 
        border-radius: 12px; 
        padding: 1rem; 
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        border: 1px solid #f0f2f6;
    }
    
    /* --- 核心修复：日志容器 --- */
    /* 给日志加一个固定高度的滚动窗口，防止页面剧烈跳动 */
    .log-scroll-container {
        max-height: 500px; /* 固定高度 */
        overflow-y: auto;  /* 内部滚动 */
        padding-right: 10px; /* 给滚动条留位置 */
        border: 1px solid #eee;
        border-radius: 8px;
        padding: 10px;
        background-color: #fafafa;
    }

    /* --- 卡片通用样式 (修复超出边框) --- */
    .log-card {
        margin-bottom: 12px;
        padding: 12px;
        border-radius: 8px;
        font-size: 0.95em;
        line-height: 1.5;
        /* 关键：强制换行，防止撑爆容器 */
        word-wrap: break-word;
        overflow-wrap: anywhere; 
        box-shadow: 0 1px 2px rgba(0,0,0,0.05);
    }

    /* 思考卡片 */
    .thought-card {
        background-color: #ffffff;
        border-left: 4px solid #9e9e9e;
        border: 1px solid #e0e0e0;
        border-left-width: 4px;
    }

    /* 行动卡片 */
    .action-card {
        background-color: #f3f9ff;
        border-left: 4px solid #2196f3;
        border: 1px solid #bbdefb;
        border-left-width: 4px;
    }

    /* 观察卡片 */
    .obs-card {
        background-color: #f1f8e9;
        border-left: 4px solid #4caf50;
        border: 1px solid #c8e6c9;
        border-left-width: 4px;
        font-family: 'Menlo', 'Consolas', monospace;
        font-size: 0.85em;
    }

    /* --- 步骤徽章 --- */
    .step-badge {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 0.8em;
        font-weight: bold;
        color: white;
        margin-right: 8px;
        vertical-align: middle;
    }
    .badge-thought { background-color: #757575; }
    .badge-action { background-color: #1976d2; }
    .badge-obs { background-color: #388e3c; }
    .katex-display { margin: 0.5em 0 !important; overflow-x: auto; overflow-y: hidden; }
    </style>
    """, unsafe_allow_html=True)

def process_latex(text):
    """
    专门修复 LLM 输出的 LaTeX 在 Streamlit 中不渲染的问题
    """
    if not text: return ""
    # 1. 修复 \[ \] 和 \( \) 为 $ 格式，Streamlit 对 $ 支持更好
    text = text.replace(r"\(", "$").replace(r"\)", "$")
    text = text.replace(r"\[", "$$").replace(r"\]", "$$")

    # 2. 关键修复：确保 $$ 块公式前后有换行，否则 KaTeX 可能不触发
    text = re.sub(r'([^\n])\s*\$\$', r'\1\n\n$$', text)
    text = re.sub(r'\$\$\s*([^\n])', r'$$\n\n\1', text)

    # 3. 关键修复：在行内公式 $ 前后增加空格，防止被识别为普通文本
    # 注意：使用正则避免匹配到 $$
    text = re.sub(r'(?<!\$)\$([^\$\n]+)\$(?!\$)', r' $\1$ ', text)

    # 4. 移除 \tag{x} 里的多余反斜杠，或者将 \tag 转换为 KaTeX 兼容格式
    # 如果 \tag 导致渲染失败，可以尝试简单的替换逻辑
    return text

def format_obs_to_html(text):
    # 仅针对 Observation 部分做 HTML 格式化
    text = re.sub(r'###\s*(.*)', r'<b style="color:#1976d2;">\1</b>', text)
    return text.replace("\n", "<br>")

# --- 3. 状态管理初始化 ---
if "messages" not in st.session_state:
    st.session_state.messages = []
if "session_id" not in st.session_state:
    st.session_state.session_id = str(uuid.uuid4())
if "current_paper_id" not in st.session_state:
    st.session_state.current_paper_id = None
if "processing" not in st.session_state:
    st.session_state.processing = False  # 控制输入框锁定状态

# --- 后端配置 ---
BASE_URL = "http://localhost:8091"
import re
def format_to_html(text):
    # 1. 先把 ### 这种标题换成 <b> 标签
    # 匹配 ### 开头，直到行尾的内容
    text = re.sub(r'###\s*(.*)', r'<b style="font-size:1.1em; color:#1976d2;">\1</b>', text)

    # 2. 将 \n 换行符替换为 HTML 的 <br>，否则 HTML 会忽略换行
    text = text.replace("\n", "<br>")

    return text

if "page" not in st.session_state:
    st.session_state.page = "🔍 Research Chat"
# --- 4. 侧边栏逻辑 ---
with st.sidebar:
    st.title("🎓 ScholarBrain")
    st.caption("v1.0.0 | 沉浸式科研助手")

    st.markdown("### 🛠️ 核心功能")
    selection = st.radio(
        "选择操作模式",
        ["🔍 Research Chat", "📤 Upload & Management"],
        index=0 if st.session_state.page == "🔍 Research Chat" else 1,
        label_visibility="collapsed"
    )
    st.session_state.page = selection

    st.markdown("---")
    # 保留原来的清理对话等按钮
    if st.button("🧹 清空当前对话", width='stretch'):
        st.session_state.messages = []
        st.session_state.session_id = str(uuid.uuid4())
        st.rerun()

if st.session_state.page == "📤 Upload & Management":
    st.header("📚 知识库管理")
    if "uploader_key" not in st.session_state:
        st.session_state.uploader_key = str(uuid.uuid4())
    # 选项卡：上传 vs 列表
    tab1, tab2 = st.tabs(["📤 上传新文档", "📑 文档列表与状态"])

    with tab1:
        st.subheader("上传 PDF")
        uploaded_file = st.file_uploader(
            "拖拽文件至此",
            type="pdf",
            key=st.session_state.uploader_key  # <--- 关键点：绑定 Key
        )
        if uploaded_file:
            # 复用你之前的上传逻辑
            with st.status("🔄 Librarian 正在入库...", expanded=True) as status:
                try:
                    files = {"file": (uploaded_file.name, uploaded_file.getvalue(), "application/pdf")}
                    response = requests.post(f"{BASE_URL}/api/v1/paper/upload", files=files)
                    if response.status_code == 200:
                        status.update(label="✅ 上传并解析成功", state="complete")
                        st.balloons()
                        st.session_state.uploader_key = str(uuid.uuid4())
                        import time
                        time.sleep(1)
                        st.rerun()
                    else:
                        status.update(label="❌ 失败", state="error")
                except Exception as e:
                    st.error(f"连接失败: {e}")

    with tab2:
        st.subheader("📑 知识库全景")
        # 刷新按钮
        if st.button("🔄 刷新列表"):
            st.rerun()

        try:
            # 调用后端 list 接口
            res = requests.get(f"{BASE_URL}/api/v1/paper/list")
            if res.status_code == 200:
                papers = res.json().get("data", [])
                if papers:
                    df = pd.DataFrame(papers)
                    # 美化列名
                    df.columns = ["id", "title", "status", "fingerprint", "createdAt"]

                    # 使用 streamlit 的 dataframe 展示，支持搜索和排序
                    status_colors = {
                        "COMPLETED": "✅",
                        "AUDITING": "⏳",
                        "PARSED": "📄",
                        "ERROR": "❌",
                    }

                    # 2. 应用映射，生成一个新的包含 Markdown 语法的列
                    df["状态"] = df["status"].apply(
                        lambda x: f"{status_colors.get(x, 'gray')}{x}"
                    )

                    # 3. 在 st.dataframe 中展示新列
                    event = st.dataframe(
                        df[["状态", "title", "createdAt","id"]], # 把“状态”放在第一列
                        width='stretch',
                        on_select="rerun",
                        selection_mode="single-row",
                        column_config={
                            "状态": st.column_config.TextColumn(
                                "状态",
                            ),
                            "title": st.column_config.TextColumn("标题"),
                            "createdAt": st.column_config.DatetimeColumn(
                                "入库时间",
                                format="YYYY-MM-DD HH:mm:ss" # 格式化时间
                            ),
                            "id": st.column_config.NumberColumn("ID", format="%d")
                        }
                    )
                    if event.selection.rows:
                        selected_index = event.selection.rows[0]
                        selected_paper_id = df.iloc[selected_index]["id"] # 假设后端返回字段叫 id

                        st.divider()
                        # ⏳ 加载动画
                        with st.spinner(f"正在调取 Paper #{selected_paper_id} 的详细档案..."):
                            try:
                                # 调用你刚写的接口
                                detail_res = requests.get(f"{BASE_URL}/api/v1/paper/{selected_paper_id}/details")

                                if detail_res.status_code == 200:
                                    # 提取 VO 数据
                                    data = detail_res.json().get("data", {})

                                    # --- 第一部分：宏观指标 (Metrics) ---
                                    st.markdown(f"### 📄 {data.get('title', 'Untitled')}")

                                    c1, c2, c3 = st.columns(3)
                                    c1.metric("📚 引用文献数", data.get('referenceCount', 0))
                                    c2.metric("🔣 提取符号数", data.get('symbolCount', 0))


                                    st.markdown("---")

                                    # --- 第二部分：Librarian 审计报告 (核心亮点) ---
                                    col_audit, col_abstract = st.columns([1, 1])

                                    with col_audit:
                                        st.subheader("🦉 Librarian 审计报告")

                                        raw_text = data.get('noveltyAssessment', '')

                                        # --- 1. 处理空状态或无关系 ---
                                        if "found no direct" in raw_text or not raw_text:
                                            st.info("🦉 Librarian 尚未发现与其他论文的直接关联。", icon="ℹ️")

                                        else:
                                            # --- 2. 解析 Markdown 文本 ---
                                            # 后端格式是： "**标题**\n\n- This paper ...\n- This paper ..."
                                            # 我们先去掉标题，然后按 "- This paper" 拆分

                                            # 渲染标题
                                            st.markdown("**🔗 知识图谱关联分析:**")

                                            # 简单的文本拆分逻辑 (根据你的 Java 格式)
                                            # 使用 split 切割成独立的条目
                                            # "Filter" 去掉空字符串
                                            relations = [r for r in raw_text.split("- This paper") if r.strip() and "**🔗" not in r]

                                            if not relations:
                                                # 如果切分失败（可能是格式变了），就兜底显示原文
                                                st.markdown(raw_text)

                                            # --- 3. 逐条渲染颜色 ---
                                            for rel in relations:
                                                # 补全被切掉的开头，组成完整的句子
                                                full_text = f"**This paper** {rel.strip()}"
                                                rel_upper = full_text.upper()

                                                # 🟢 绿色类：支持、扩展、基石
                                                if any(k in rel_upper for k in ["EXTEND", "SUPPORT", "FOUNDATIONAL", "BASIS"]):
                                                    st.success(full_text, icon="✅")

                                                # 🔴 红色类：冲突、反驳
                                                elif any(k in rel_upper for k in ["CONFLICT", "CONTRADICT", "REFUTE"]):
                                                    st.error(full_text, icon="⚠️")

                                                # 🔵 蓝色类：其他/替代
                                                else:
                                                    st.info(full_text, icon="ℹ️")

                                    with col_abstract:
                                        st.subheader("📝 摘要")
                                        abstract = data.get('abstractText', '暂无摘要')
                                        # 摘要过长折叠
                                        with st.container(height=200):
                                            st.markdown(abstract)

                                    st.markdown("---")

                                    # --- 第三部分：核心符号预览 (Key Symbols) ---
                                    st.subheader("🔣 核心数学定义 (Top 5)")

                                    key_symbols = data.get('keySymbols', [])
                                    if key_symbols:
                                        # 使用 Markdown 表格展示，支持 LaTeX
                                        md_table = "| 符号 (Symbol) | 含义 (Description) | 定义公式 (Formula) |\n|---|---|---|\n"
                                        for sym in key_symbols:
                                            # 处理 null
                                            latex = sym.get('latex') or sym.get('symbol')
                                            desc = sym.get('description', '-')
                                            formula = sym.get('definitionFormula')
                                            formula_str = f"${formula}$" if formula else "-"

                                            md_table += f"| ${latex}$ | {desc} | {formula_str} |\n"

                                        st.markdown(md_table)
                                    else:
                                        st.caption("该论文未提取到数学符号，或尚未完成解析。")

                                else:
                                    st.error(f"获取详情失败: {detail_res.text}")

                            except Exception as e:
                                st.error(f"连接错误: {str(e)}")
        except Exception as e:
            st.error(f"无法获取列表: {e}")


elif st.session_state.page == "🔍 Research Chat":
    # --- 5. 主聊天区域 ---
    st.markdown("#### 🔬 沉浸式科研工作台")

    # 渲染历史消息
    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(process_latex(message["content"]))

    # --- 6. 处理新输入 (核心逻辑改造) ---

    # 使用回调函数处理输入，或者直接判断
    # 这里的 disabled=st.session_state.processing 实现了“提问时锁住输入框”
    prompt = st.chat_input("输入你的问题 (例如: 这篇论文的核心创新点是什么?)", disabled=st.session_state.processing)

    if prompt:
        # 1. 立即锁定界面
        st.session_state.processing = True

        # 2. 记录用户问题
        st.session_state.messages.append({"role": "user", "content": prompt})
        with st.chat_message("user"):
            st.markdown(prompt)

        # 3. 开始助手响应
        with st.chat_message("assistant"):
            # 动态状态面板

            status_container = st.status("🚀 启动思维引擎...", expanded=True)

            # 在容器内部创建占位符，用于流式渲染日志
            log_placeholder = status_container.empty()

            # 答案占位符
            answer_placeholder = st.empty()

            # 日志累加器
            full_logs_html= ""
            final_answer = ""

            try:
                url = f"{BASE_URL}/api/v1/agent/ask-stream"
                params = {
                    "sessionId": st.session_state.session_id,
                    "question": prompt
                }

                response = requests.get(url, params=params, stream=True, timeout=600) # 长超时
                response.encoding = 'utf-8'

                client = SSEClient(response)

                for event in client.events():
                    if not event.data: continue

                    try:
                        event_data = json.loads(event.data)

                        # --- 提取字段 ---
                        msg_type = event_data.get("type")
                        content = event_data.get("content", "")
                        step = event_data.get("step", 0) # 获取 Step

                        # --- 动态更新状态标题 ---
                        if str(step).isdigit():
                            status_container.update(label=f"🔄 ScholarBrain 思考中... [第 {step} 步]", state="running")
                        new_html = ""

                        if msg_type == "THOUGHT":
                            # 转义内容，防止 HTML 注入破坏格式
                            safe_content = html.escape(content).replace("\n", "<br>")
                            new_html =  textwrap.dedent( f"""
                                <div class="log-card thought-card">
                                    <div>
                                        <span class="step-badge badge-thought">STEP {step}</span>
                                        <b>Thought</b>
                                    </div>
                                    <div style="margin-top:5px; color:#333;">{safe_content}</div>
                                </div>
                            """)

                        elif msg_type == "ACTION":
                            raw_data = event_data.get("data", "")
                            action_input = html.escape(json.dumps(raw_data, ensure_ascii=False) if isinstance(raw_data, (dict, list)) else str(raw_data))
                            safe_content = html.escape(content)

                            new_html = textwrap.dedent(f"""
                                <div class="log-card action-card">
                                    <div>
                                        <span class="step-badge badge-action">STEP {step}</span>
                                        <b>Action:</b> <code>{safe_content}</code>
                                    </div>
                                    <div style="margin-top:5px; font-size:0.9em; color:#555;">
                                        <b>Input:</b> <code>{action_input}</code>
                                    </div>
                                </div>
                            """)

                        elif msg_type == "OBSERVATION":
                            display_content = content[:3000] + "..." if len(content) > 3000 else content
                            safe_content = format_to_html(display_content)

                            new_html = textwrap.dedent(f"""
                                <div class="log-card obs-card">
                                    <div>
                                        <span class="step-badge badge-obs">STEP {step}</span>
                                        <b>Observation</b>
                                    </div>
                                    <div style="margin-top:5px; white-space: pre-wrap;">{safe_content}</div>
                                </div>
                            """)

                        elif msg_type == "ANSWER" or msg_type == "FINAL_ANSWER_GENERATED":
                            status_container.update(label="✅ 思考完成", state="complete", expanded=False)
                            final_answer = process_latex(content)
                            answer_placeholder.markdown(final_answer)
                            continue

                        if new_html:
                            full_logs_html += new_html

                            log_placeholder.markdown(
                                f"""
                                <div class="log-scroll-container">
                                    {full_logs_html}
                                </div>
                                """,
                                unsafe_allow_html=True
                            )

                    except json.JSONDecodeError:
                        pass

                # --- 4. 请求结束处理 ---
                if final_answer:
                    st.session_state.messages.append({"role": "assistant", "content": final_answer})
                else:
                    # 如果没有最终答案（比如超时或报错），给个提示
                    if not final_answer:
                        st.error("未收到最终回复，请检查后台日志。")

            except Exception as e:
                st.error(f"连接中断: {str(e)}")

            finally:
                # --- 5. 解锁输入框 ---
                st.session_state.processing = False
                # 强制刷新以使 disabled=False 生效
                st.rerun()
