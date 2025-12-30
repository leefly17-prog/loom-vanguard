#!/bin/bash

# 替换脚本：将 leefly17-prog 替换为实际的 GitHub 用户名
# 使用方法: ./replace_username.sh <your-github-username> [your-email]

set -e

if [ -z "$1" ]; then
    echo "使用方法: ./replace_username.sh <your-github-username> [your-email]"
    echo ""
    echo "示例:"
    echo "  ./replace_username.sh yifei"
    echo "  ./replace_username.sh yifei yifei@example.com"
    exit 1
fi

GITHUB_USERNAME="$1"
EMAIL="${2:-your.email@example.com}"

echo "=========================================="
echo "替换配置信息"
echo "=========================================="
echo "GitHub 用户名: $GITHUB_USERNAME"
echo "邮箱: $EMAIL"
echo ""

# 确认
read -p "确认替换? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 1
fi

# 需要替换的文件（排除 target 目录和 .git 目录）
FILES=$(find . -type f \( -name "*.java" -o -name "*.xml" -o -name "*.md" -o -name "*.sh" -o -name "*.yml" \) \
    ! -path "./target/*" ! -path "./.git/*" ! -path "./.idea/*" ! -path "./.vscode/*")

echo "正在替换..."

# 替换 leefly17-prog 为实际的 GitHub 用户名
for file in $FILES; do
    if [ -f "$file" ]; then
        # macOS 使用 sed -i ''，Linux 使用 sed -i
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s/leefly17-prog/$GITHUB_USERNAME/g" "$file"
            sed -i '' "s/your\.email@example\.com/$EMAIL/g" "$file"
        else
            sed -i "s/leefly17-prog/$GITHUB_USERNAME/g" "$file"
            sed -i "s/your\.email@example\.com/$EMAIL/g" "$file"
        fi
    fi
done

# 重命名目录结构
echo "正在重命名目录结构..."

MAIN_DIR="src/main/java/io/github/$GITHUB_USERNAME"
TEST_DIR="src/test/java/io/github/$GITHUB_USERNAME"

if [ -d "src/main/java/io/github/leefly17-prog" ]; then
    mkdir -p "$MAIN_DIR"
    mv "src/main/java/io/github/leefly17-prog/loomvanguard" "$MAIN_DIR/" 2>/dev/null || true
    rm -rf "src/main/java/io/github/leefly17-prog" 2>/dev/null || true
fi

if [ -d "src/test/java/io/github/leefly17-prog" ]; then
    mkdir -p "$TEST_DIR"
    mv "src/test/java/io/github/leefly17-prog/loomvanguard" "$TEST_DIR/" 2>/dev/null || true
    rm -rf "src/test/java/io/github/leefly17-prog" 2>/dev/null || true
fi

echo ""
echo "=========================================="
echo "替换完成！"
echo "=========================================="
echo ""
echo "已替换的内容:"
echo "  - GitHub 用户名: leefly17-prog -> $GITHUB_USERNAME"
echo "  - 邮箱: your.email@example.com -> $EMAIL"
echo "  - 目录结构已重命名"
echo ""
echo "下一步:"
echo "  1. 运行 'mvn clean compile' 验证编译"
echo "  2. 运行 'mvn test' 验证测试"
echo "  3. 检查代码，确认替换正确"
echo ""

