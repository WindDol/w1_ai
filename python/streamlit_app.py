import streamlit as st
import requests
import json
import uuid
from sseclient import SSEClient

# --- 1. 页面基础配置 ---
st.set_page_config(
    page_title="ScholarBrain 2.0 - 深度科研",
    page_icon="🎓",
    layout="wide",
    initial_sidebar_state="expanded"
)

# --- 2. CSS 深度美化 ---
st.markdown("""
    <style>
    /* 全局字体优化 */
    .stApp { font-family: 'Inter', system-ui, sans-serif; }
    
    /* 聊天气泡样式 */
    .stChatMessage { 
        border-radius: 12px; 
        padding: 1rem; 
        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
        border: 1px solid #f0f2f6;
    }
    
    /* 思维链日志样式 */
    .thought-card {
        background-color: #f8f9fa;
        border-left: 3px solid #6c757d;
        padding: 10px;
        margin: 5px 0;
        border-radius: 0 5px 5px 0;
        font-size: 0.9em;
    }
    .action-card {
        background-color: #e3f2fd;
        border-left: 3px solid #2196f3;
        padding: 10px;
        margin: 5px 0;
        border-radius: 0 5px 5px 0;
        font-size: 0.9em;
    }
    .obs-card {
        background-color: #e8f5e9;
        border-left: 3px solid #4caf50;
        padding: 10px;
        margin: 5px 0;
        border-radius: 0 5px 5px 0;
        font-size: 0.85em;
        font-family: monospace;
        white-space: pre-wrap; /* 保持换行 */
    }
    
    /* 标题样式 */
    h1, h2, h3 { color: #2c3e50; }
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
    with col2:
        st.link_button("🐞 报Bug", "https://github.com/your-repo/issues", use_container_width=True)

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
        status_header = st.empty() # 用于动态更新 "第N步..."
        status_container = st.status("🚀 启动思维引擎...", expanded=True)

        # 在容器内部创建占位符，用于流式渲染日志
        log_placeholder = status_container.empty()

        # 答案占位符
        answer_placeholder = st.empty()

        # 日志累加器
        full_logs = ""
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
                    if step > 0:
                        status_container.update(label=f"🔄 ScholarBrain 思考中... [第 {step} 步]", state="running")

                    # --- 根据类型渲染美化后的 HTML ---
                    if msg_type == "THOUGHT":
                        # 使用 HTML div 包装，实现自定义样式
                        new_log = f"""
                        <div class="thought-card">
                            <b>🤔 Thought (Step {step}):</b><br>{content}
                        </div>
                        """
                        full_logs += new_log
                        log_placeholder.markdown(full_logs, unsafe_allow_html=True)

                    elif msg_type == "ACTION":
                        action_input = event_data.get("data", "")
                        new_log = f"""
                        <div class="action-card">
                            <b>🛠️ Action:</b> <code>{content}</code><br>
                            <span style="color:#666;font-size:0.8em">Input: {action_input}</span>
                        </div>
                        """
                        full_logs += new_log
                        log_placeholder.markdown(full_logs, unsafe_allow_html=True)

                    elif msg_type == "OBSERVATION":
                        # 截断过长内容用于显示
                        display_content = content[:800] + "..." if len(content) > 800 else content
                        # 转义 HTML 字符防止渲染破坏
                        import html
                        display_content = html.escape(display_content)

                        new_log = f"""
                        <div class="obs-card"><b>👁️ Observation:</b><br>{display_content}</div>
                        """
                        full_logs += new_log
                        log_placeholder.markdown(full_logs, unsafe_allow_html=True)

                    elif msg_type == "ANSWER" or msg_type == "FINAL_ANSWER_GENERATED":
                        # 思考结束，收起面板
                        status_container.update(label="✅ 思考完成", state="complete", expanded=False)
                        final_answer = content
                        # 流式输出最终答案效果（可选，这里直接显示）
                        answer_placeholder.markdown(final_answer)

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