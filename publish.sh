#!/bin/bash

# Loom Vanguard 发布脚本
# 用于快速将项目发布到 GitHub 和 JitPack

set -e

echo "=========================================="
echo "Loom Vanguard 发布脚本"
echo "=========================================="

# 检查是否在项目根目录
if [ ! -f "pom.xml" ]; then
    echo "错误: 请在项目根目录运行此脚本"
    exit 1
fi

# 检查 git 是否已初始化
if [ ! -d ".git" ]; then
    echo "初始化 Git 仓库..."
    git init
    echo "✅ Git 仓库已初始化"
else
    echo "✅ Git 仓库已存在"
fi

# 检查是否有未提交的更改
if [ -n "$(git status --porcelain)" ]; then
    echo "检测到未提交的更改，是否提交？(y/n)"
    read -r response
    if [ "$response" = "y" ]; then
        echo "请输入提交信息（直接回车使用默认信息）:"
        read -r commit_msg
        if [ -z "$commit_msg" ]; then
            commit_msg="Update: Loom Vanguard"
        fi
        git add .
        git commit -m "$commit_msg"
        echo "✅ 更改已提交"
    fi
else
    echo "✅ 没有未提交的更改"
fi

# 检查远程仓库
if ! git remote | grep -q origin; then
    echo ""
    echo "请提供 GitHub 仓库地址（例如: https://github.com/leefly17-prog/loom-vanguard.git）"
    echo "或者直接回车跳过（稍后手动添加）:"
    read -r remote_url
    
    if [ -n "$remote_url" ]; then
        git remote add origin "$remote_url"
        echo "✅ 远程仓库已添加: $remote_url"
    else
        echo "⚠️  未添加远程仓库，请稍后手动添加:"
        echo "   git remote add origin https://github.com/leefly17-prog/loom-vanguard.git"
    fi
else
    echo "✅ 远程仓库已配置"
    git remote -v | grep origin
fi

# 获取当前版本
VERSION=$(grep -A 1 "<version>" pom.xml | head -2 | tail -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')
echo ""
echo "当前版本: $VERSION"

# 询问是否创建 tag
echo ""
echo "是否创建并推送 tag v$VERSION? (y/n)"
read -r create_tag

if [ "$create_tag" = "y" ]; then
    # 检查 tag 是否已存在
    if git tag | grep -q "v$VERSION"; then
        echo "⚠️  Tag v$VERSION 已存在，是否删除并重新创建? (y/n)"
        read -r recreate
        if [ "$recreate" = "y" ]; then
            git tag -d "v$VERSION" 2>/dev/null || true
            git push origin ":refs/tags/v$VERSION" 2>/dev/null || true
        else
            echo "跳过 tag 创建"
            create_tag="n"
        fi
    fi
    
    if [ "$create_tag" = "y" ]; then
        git tag -a "v$VERSION" -m "Release version $VERSION"
        echo "✅ Tag v$VERSION 已创建"
    fi
fi

# 询问是否推送到 GitHub
echo ""
echo "是否推送到 GitHub? (y/n)"
read -r push_code

if [ "$push_code" = "y" ]; then
    # 检查是否有 main 分支
    if ! git branch | grep -q "main"; then
        if git branch | grep -q "master"; then
            git branch -M master main
        else
            git checkout -b main
        fi
    fi
    
    echo "推送代码到 GitHub..."
    git push -u origin main || git push origin main
    
    if [ "$create_tag" = "y" ]; then
        echo "推送 tag 到 GitHub..."
        git push origin "v$VERSION" || git push origin "v$VERSION" --tags
    fi
    
    echo "✅ 代码已推送到 GitHub"
fi

echo ""
echo "=========================================="
echo "发布完成！"
echo "=========================================="
echo ""
echo "📦 使用 JitPack 引入:"
echo ""
echo "1. 访问 https://jitpack.io/#leefly17-prog/loom-vanguard"
echo "2. 等待构建完成（约 1-2 分钟）"
echo "3. 在其他项目的 pom.xml 中添加:"
echo ""
echo "   <repositories>"
echo "       <repository>"
echo "           <id>jitpack.io</id>"
echo "           <url>https://jitpack.io</url>"
echo "       </repository>"
echo "   </repositories>"
echo ""
echo "   <dependencies>"
echo "       <dependency>"
echo "           <groupId>com.github.leefly17-prog</groupId>"
echo "           <artifactId>loom-vanguard</artifactId>"
echo "           <version>$VERSION</version>"
echo "       </dependency>"
echo "   </dependencies>"
echo ""
echo "⚠️  记得将 'leefly17-prog' 替换为你的 GitHub 用户名！"
echo ""

