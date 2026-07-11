# MonkeyOCR Parser Setup

ScholarBrain can use MonkeyOCR as the PDF parser through the existing Java ingest flow.

## Remote 4090 over SSH tunnel

Run MonkeyOCR on the remote GPU host:

```bash
cd /root/autodl-tmp/MonkeyOCR
conda activate monkeyocr
export OMP_NUM_THREADS=1
uvicorn api.main:app --host 0.0.0.0 --port 8000
```

Forward the remote API to your local machine:

```powershell
ssh -N -L 8000:127.0.0.1:8000 root@<autodl-ssh-host> -p <autodl-ssh-port>
```

Verify locally:

```powershell
curl http://127.0.0.1:8000/health
curl -X POST "http://127.0.0.1:8000/parse" -F "file=@C:\path\to\paper.pdf"
```

Start the Java app with:

```bash
export PDF_PARSER_TYPE=monkeyocr-http
export MONKEYOCR_BASE_URL=http://127.0.0.1:8000
export MONKEYOCR_HTTP_OUTPUT_DIR=./data/monkeyocr-http-output
export MONKEYOCR_HTTP_TIMEOUT_SECONDS=1800
```

The HTTP adapter uploads the PDF to `/parse`, downloads the returned `download_url` zip,
extracts it locally, reads the generated Markdown file, and then passes it through:

```text
PaperStructureNormalizer -> MarkdownParser -> sections/references/symbols/embedding
```

## Local command mode

If MonkeyOCR is installed on the same machine as the Java app, set these variables before starting:

```bash
export PDF_PARSER_TYPE=monkeyocr
export MONKEYOCR_REPO_PATH=/root/autodl-tmp/MonkeyOCR
export MONKEYOCR_PYTHON=/root/miniconda3/bin/python
export MONKEYOCR_OUTPUT_DIR=/root/autodl-tmp/scholarbrain-monkeyocr-output
export MONKEYOCR_OMP_NUM_THREADS=1
```

Optional:

```bash
export MONKEYOCR_SCRIPT_PATH=parse.py
export MONKEYOCR_CONFIG_PATH=/root/autodl-tmp/MonkeyOCR/configs/model_configs.yaml
export MONKEYOCR_TIMEOUT_SECONDS=1800
```

The adapter runs:

```bash
$MONKEYOCR_PYTHON $MONKEYOCR_REPO_PATH/parse.py <pdf> -o $MONKEYOCR_OUTPUT_DIR
```

Then it reads the newest generated Markdown file and passes it through:

```text
PaperStructureNormalizer -> MarkdownParser -> sections/references/symbols/embedding
```

## Fallback

To use the old LlamaParse script instead:

```bash
export PDF_PARSER_TYPE=llama
export LLAMA_PARSE_API_KEY=...
export LLAMA_PARSE_SCRIPT=/path/to/w1_ai/python/paper_parser.py
export PYTHON_EXECUTABLE=python
```

Never put API keys in this file. Configure secrets through environment variables or the ignored
`config/application-local.yml` file. Keys that have ever been committed or shared must be rotated.
