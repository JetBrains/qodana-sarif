# Qodana Sarif Converter

## Run converter

Use this script to run the converter:

```bash
docker run \
  --pull always \
  -v "$(pwd)/sarif.json:/data/sarif.json" \
  -v "$(pwd)/results:/data/results" \
  -p 8080:8080 \
  registry.jetbrains.team/p/sa/containers/qodana-sarif-converter:latest
```

Following flags are supported:
- `-s` to serve ui after conversion
