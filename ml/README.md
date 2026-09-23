# EKT.kz AI Product Retrieval & Assistant

AI/ML module for the HackAlem AI hackathon case:

**"AI Assistant for Chat on ekt.kz"**

The module provides intelligent product search over the EKT.kz catalog and serves as the retrieval layer for the conversational AI assistant.

## Overview

The EKT.kz catalog contains more than 15,000 electrical products with technical names, abbreviations, product codes, specifications, prices, and stock information.

Users may search using natural language:

```text
Мне нужен трехфазный автомат Legrand на 40 ампер
```

while the catalog may contain:

```text
027004 АВ DRX125 MT 3ф 40А 10ka Legrand
```

Simple keyword or semantic search is not reliable enough for technical product retrieval.

The implemented solution uses a two-stage hybrid retrieval pipeline combining:

- Query parsing
- Fuzzy matching
- Lexical matching
- Semantic embeddings
- FAISS vector search
- Technical attribute extraction
- Product data normalization
- Live EKT.kz API data
- Attribute-based reranking

## Architecture

```text
User Query
    |
    v
Query Parser
    |
    v
+--------------------------+
| Stage 1 Retrieval        |
|                          |
| 15,035 catalog products  |
|                          |
| - Fuzzy Search           |
| - Semantic Search        |
| - Lexical Search         |
| - Attribute Matching     |
+------------+-------------+
             |
             v
        Top Candidates
             |
             v
       EKT Detail API
             |
             v
       Product Normalizer
             |
             v
+--------------------------+
| Stage 2 Reranking        |
|                          |
| - Brand                  |
| - Current                |
| - Number of poles        |
| - Voltage                |
| - Breaking capacity      |
+------------+-------------+
             |
             v
          Top 5
             |
             v
       AI Tool Layer
             |
             v
         OpenAI LLM
             |
             v
      User-facing answer
```

## Why Hybrid Retrieval?

During development, several retrieval approaches were tested.

### Fuzzy Search

Fuzzy matching worked well for exact technical identifiers such as:

```text
DRX125 40A
```

but performed poorly for natural-language queries.

Example:

```text
трехфазный автомат Legrand 40 ампер
```

could return unrelated Legrand products.

### Semantic Search

Semantic search using multilingual sentence embeddings was also tested.

Raw product names contain many technical abbreviations:

```text
АВ
3ф
3P
40А
10kA
DRX125
```

Semantic embeddings alone did not reliably preserve exact technical constraints such as current, voltage, poles, or breaking capacity.

### Hybrid Search

The final approach combines semantic retrieval with explicit technical attribute matching.

Example query:

```text
автомат Legrand 3P 40A 400V 10kA
```

Parsed representation:

```json
{
  "product_type": "circuit_breaker",
  "brand": "Legrand",
  "current": 40,
  "poles": 3,
  "voltage": 400,
  "breaking_capacity": 10
}
```

The system then combines retrieval scores with exact attribute matching.

This allows exact technical requirements to have more influence than semantic similarity alone.

## Example Result

Query:

```text
автомат Legrand 3P 40A 400V 10kA
```

Top result:

```text
027004 АВ DRX125 MT 3ф 40А 10ka Legrand

ID: 515279
Brand: Legrand
Current: 40 A
Poles: 3
Voltage: 400 V
Breaking capacity: 10 kA
Price: 26930
Quantity: 36
```

Matched attributes:

```text
brand exact
current exact
poles exact
voltage exact
breaking capacity exact
```

## Project Structure

```text
ml/
├── ai_agent.py
├── analyze_properties.py
├── build_embeddings.py
├── build_normalized_embeddings.py
├── detail_client.py
├── download_details.py
├── download_products.py
├── hybrid_search.py
├── normalizer.py
├── query_parser.py
├── search.py
├── semantic_search.py
├── stage1_search.py
├── stage2_search.py
├── tools.py
├── test_openai.py
├── test_tools.py
├── tests/
│   └── test_queries.json
└── data/
```

## Main Components

### `download_products.py`

Downloads the product catalog from the EKT API using pagination.

The current catalog contains approximately:

```text
15,035 products
```

The downloader supports checkpointing and retry behavior.

### `query_parser.py`

Extracts structured technical constraints from natural-language queries.

Example:

```text
Мне нужен автомат Legrand для трехфазной сети на 40 ампер
```

becomes:

```json
{
  "product_type": "circuit_breaker",
  "brand": "Legrand",
  "current": 40,
  "poles": 3
}
```

The parser understands several forms of technical notation, including:

```text
40A
40А
40 ампер

3P
3П
3ф
трехфазный
трёхфазной

400V
400В
400 вольт

10kA
10кА
```

### `normalizer.py`

Transforms raw EKT product data into a consistent internal representation.

Example:

```json
{
  "id": 515279,
  "brand": "Legrand",
  "current": 40,
  "poles": 3,
  "voltage": 400,
  "breaking_capacity": 10
}
```

The normalizer also detects conflicts between product fields.

For example, one catalog item contained:

```text
Product name: 40A
Description: 40A
Structured property: 125A
```

Instead of silently trusting one field, the system records the inconsistency in:

```json
{
  "conflicts": [
    {
      "field": "current",
      "property_value": 125,
      "text_value": 40
    }
  ]
}
```

### `stage1_search.py`

Generates candidate products from the complete catalog.

It combines:

```text
Fuzzy Search
+
Semantic Search
+
Lexical Matching
+
Attribute Candidate Generation
```

The objective of Stage 1 is high recall: the correct product should remain among the candidates.

### `detail_client.py`

Retrieves current product information from the EKT detail API.

This includes information such as:

- Price
- Total stock
- Stock by warehouse
- Technical properties
- Description
- Product URL
- Image

Live API calls are used because stock and price may change.

### `stage2_search.py`

Normalizes Stage 1 candidates and performs precise reranking using technical attributes.

Matching signals include:

```text
Brand
Current
Poles
Voltage
Breaking capacity
```

Explicit mismatches receive penalties.

Stage 2 returns the final Top-K products.

### `tools.py`

Provides a clean interface between the retrieval system and the LLM.

Example:

```python
from tools import product_search

result = product_search(
    "автомат Legrand 3P 40A 400V 10kA"
)
```

The caller does not need to know about FAISS, embeddings, normalization, or reranking.

### `ai_agent.py`

Conversational AI layer powered by the OpenAI API.

The LLM is responsible for understanding the conversation and selecting tools.

Product facts are retrieved through tools instead of being generated from model knowledge.

The assistant should never invent:

- Product prices
- Stock availability
- Technical specifications

## EKT API

The project uses the EKT.kz product API.

Required environment variables:

```text
EKT_USERNAME
EKT_PASSWORD
```

API credentials must never be committed to Git.

## OpenAI

The conversational layer uses the OpenAI API.

Required environment variable:

```text
OPENAI_API_KEY
```

The key must be stored locally in `.env`.

Example:

```text
EKT_USERNAME=...
EKT_PASSWORD=...
OPENAI_API_KEY=...
```

Do not commit `.env`.

## Installation

Create and activate a virtual environment:

```bash
python3 -m venv venv
source venv/bin/activate
```

Install dependencies:

```bash
python -m pip install \
    requests \
    python-dotenv \
    numpy \
    rapidfuzz \
    faiss-cpu \
    sentence-transformers \
    openai
```

## Build Product Catalog

Download the catalog:

```bash
python download_products.py
```

Build embeddings:

```bash
python build_embeddings.py
```

## Run Retrieval

Stage 1:

```bash
python stage1_search.py
```

Full two-stage retrieval:

```bash
python stage2_search.py
```

Example query:

```text
автомат Legrand 3P 40A 400V 10kA
```

## Test Tool Layer

```bash
python test_tools.py
```

## Test OpenAI API

```bash
python test_openai.py
```

Expected output:

```text
API works
```

## Run AI Assistant

```bash
python ai_agent.py
```

Example:

```text
You:
Мне нужен автомат Legrand 3P 40A 400V 10kA
```

The assistant calls the product retrieval tool, receives current catalog data, and generates a user-friendly response.

## Data Safety

Secrets are stored in `.env`.

Generated catalog and index files should not be committed:

```gitignore
.env
venv/
__pycache__/

data/products.json
data/product_details.json
data/product_metadata.json
data/normalized_products.json

*.index
```

## Current Status

Implemented:

```text
[x] EKT API integration
[x] Catalog downloader
[x] Query parser
[x] Fuzzy search baseline
[x] Semantic search baseline
[x] FAISS vector index
[x] Product normalization
[x] Data conflict detection
[x] Hybrid retrieval
[x] Stage 1 candidate generation
[x] Live product detail retrieval
[x] Stage 2 attribute reranking
[x] Product search tool
[x] OpenAI API integration
```

Next steps:

```text
[ ] Conversational context
[ ] Product analog recommendation
[ ] Cart confirmation workflow
[ ] Cart integration
[ ] Purchase conditions knowledge
[ ] Kazakh language support
[ ] Retrieval evaluation
[ ] API latency optimization
[ ] Detail API caching
```

## Hackathon Goal

The final assistant should allow an EKT.kz customer to:

```text
Ask about a product
        ↓
Find matching catalog items
        ↓
Check specifications
        ↓
Check live availability
        ↓
Find alternatives
        ↓
Confirm a product
        ↓
Add it to the cart
        ↓
Continue to checkout
```

The core design principle is:

> The LLM handles conversation. The retrieval system handles product facts.

This separation reduces hallucinations and keeps price, stock, and technical information grounded in EKT.kz data.
