#!/bin/sh
echo "在仓库根目录执行本脚本(Execute the script at the root of this git repository)"

SRC_DIR="pdf_client_wrapper"

pipenv run python -m grpc_tools.protoc --python_out=$SRC_DIR/rpc --grpc_python_out=$SRC_DIR/rpc \
--proto_path=grpc-proto/ grpc-proto/xy-units.proto grpc-proto/pdf.proto
echo "Success"
