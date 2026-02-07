import streamlit as st
import requests
import json
import uuid
import html  # 引入 html 库用于转义
from sseclient import SSEClient
import textwrap

# --- 1. 页面基础配置 ---
st.set_page_config(
    page_title="ScholarBrain 2.0 - 深度科研",
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

    </style>
    """, unsafe_allow_html=True)

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
# --- 4. 侧边栏逻辑 ---
with st.sidebar:
    st.title("🎓 ScholarBrain")
    st.caption("v2.0.1 | Powered by ReAct Agent")
    st.markdown("---")

    st.subheader("📄 知识库")

    # 状态指示器
    if st.session_state.current_paper_id:
        st.success(f"📚 已加载论文 ID: **{st.session_state.current_paper_id}**")
    else:
        st.info("👋 请先上传一篇 PDF")

    uploaded_file = st.file_uploader("上传新论文", type="pdf", disabled=st.session_state.processing)

    if uploaded_file:
        # 避免重复上传的简单逻辑：如果是同一个文件对象就不重复请求（Streamlit特性）
        # 这里为了演示，每次上传都触发
        with st.status("🔄 Librarian 正在入库...", expanded=True) as status:
            try:
                files = {"file": (uploaded_file.name, uploaded_file.getvalue(), "application/pdf")}
                # 使用 spinner 增加动效
                response = requests.post(f"{BASE_URL}/api/v1/paper/upload", files=files)

                if response.status_code == 200:
                    result = response.json()
                    paper_info = result.get("data", "")
                    # 提取 Paper ID
                    if "paperId为:" in paper_info:
                        pid = paper_info.split(":")[-1].strip()
                        st.session_state.current_paper_id = pid
                        status.update(label=f"✅ 入库成功! ID: {pid}", state="complete", expanded=False)
                        st.rerun() # 刷新以更新状态指示器
                else:
                    status.update(label="❌ 上传失败", state="error")
                    st.error("后端服务未响应")
            except Exception as e:
                status.update(label="❌ 连接错误", state="error")
                st.error(str(e))

    st.markdown("---")
    col1, col2 = st.columns(2)
    with col1:
        if st.button("🧹 新对话", use_container_width=True, disabled=st.session_state.processing):
            st.session_state.messages = []
            st.session_state.session_id = str(uuid.uuid4())
            st.rerun()

# --- 5. 主聊天区域 ---
st.markdown("#### 🔬 沉浸式科研工作台")

# 渲染历史消息
for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

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
                        final_answer = content
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
