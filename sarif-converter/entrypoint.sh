#!/bin/bash

serve_ui=''

run_converter() {
  /app/bin/sarif-converter -s /data/sarif.json -o /data/results
}

serve_ui() {
  if [[ -n $serve_ui ]]; then
    cp -R /data/results /app/ui
    http-server ./ui
  fi
}

print_usage() {
  printf "Usage:"
  printf "-s -- serve UI after convering"
}

while getopts 's' flag; do
  case "${flag}" in
    s) serve_ui='true' ;;
    *) print_usage
       exit 1 ;;
  esac
done

run_converter
serve_ui

