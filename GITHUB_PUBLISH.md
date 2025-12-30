# GitHub 发布指南

## 📦 方案一：使用 JitPack（推荐，最简单）

JitPack 是最简单的方案，只需要将代码推送到 GitHub，就能自动构建和发布。

### 步骤 1：初始化 Git 仓库

```bash
cd /Users/yifei/Desktop/loom-vanguard

# 初始化 git
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit: Loom Vanguard - Virtual thread dispatcher with backpressure control"
```

### 步骤 2：在 GitHub 创建仓库

1. 访问 https://github.com/new
2. 仓库名称：`loom-vanguard`
3. 描述：`A high-performance virtual thread dispatcher with backpressure control using semaphores`
4. 选择 Public（公开）
5. **不要**勾选 "Initialize this repository with a README"（因为本地已有）
6. 点击 "Create repository"

### 步骤 3：推送代码到 GitHub

```bash
# 添加远程仓库（替换 leefly17-prog 为你的 GitHub 用户名）
git remote add origin https://github.com/leefly17-prog/loom-vanguard.git

# 推送代码
git branch -M main
git push -u origin main
```

### 步骤 4：创建 Release Tag

```bash
# 创建 tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送 tag
git push origin v1.0.0
```

### 步骤 5：使用 JitPack

JitPack 会自动构建你的项目。其他项目可以这样使用：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.leefly17-prog</groupId>
        <artifactId>loom-vanguard</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**注意**：将 `leefly17-prog` 替换为你的 GitHub 用户名。

### JitPack 的优势

✅ **零配置**：不需要任何额外配置  
✅ **自动构建**：推送到 GitHub 后自动构建  
✅ **版本管理**：通过 Git tag 管理版本  
✅ **免费**：完全免费使用  

---

## 📦 方案二：发布到 Maven Central（正式方案）

如果要发布到 Maven Central，需要更多步骤，但这是最正式的发布方式。

### 前置要求

1. **Sonatype 账号**：在 https://issues.sonatype.org 注册账号
2. **GPG 密钥**：用于签名
3. **域名验证**：需要验证你拥有 `io.github.leefly17-prog` 域名（GitHub 会自动验证）

### 步骤 1：配置 pom.xml

需要在 `pom.xml` 中添加发布配置：

```xml
<distributionManagement>
    <snapshotRepository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>

<build>
    <plugins>
        <!-- ... 现有插件 ... -->
        
        <!-- GPG 签名插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <version>3.0.1</version>
            <executions>
                <execution>
                    <id>sign-artifacts</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>sign</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        
        <!-- 发布插件 -->
        <plugin>
            <groupId>org.sonatype.central</groupId>
            <artifactId>central-publishing-maven-plugin</artifactId>
            <version>0.5.0</version>
            <extensions>true</extensions>
            <configuration>
                <publishingServerId>ossrh</publishingServerId>
                <autoPublish>true</autoPublish>
                <waitUntil>published</waitUntil>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 步骤 2：配置 Maven settings.xml

在 `~/.m2/settings.xml` 中添加：

```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>你的 Sonatype 用户名</username>
            <password>你的 Sonatype 密码</password>
        </server>
    </servers>
</settings>
```

### 步骤 3：发布

```bash
# 清理并构建
mvn clean

# 发布到 Maven Central
mvn deploy
```

### Maven Central 的优势

✅ **官方仓库**：最正式的 Maven 仓库  
✅ **无需额外配置**：其他项目直接使用，无需添加 repository  
✅ **长期稳定**：Maven Central 是最稳定的仓库  

---

## 🎯 推荐方案

### 对于个人项目/快速发布：使用 JitPack

- ✅ 最简单，5 分钟搞定
- ✅ 零配置
- ✅ 适合快速迭代

### 对于正式项目/长期维护：使用 Maven Central

- ✅ 最正式
- ✅ 无需额外配置（对使用者）
- ✅ 适合长期维护的项目

---

## 📝 更新 README

发布后，记得更新 README.md 中的：

1. **GitHub 链接**：将 `leefly17-prog` 替换为你的用户名
2. **Maven 依赖**：根据选择的方案更新依赖说明
3. **Badge 链接**：更新 CI/CD badge

---

## 🚀 快速开始（JitPack 方案）

如果你想快速发布，推荐使用 JitPack：

```bash
# 1. 初始化 git
git init
git add .
git commit -m "Initial commit"

# 2. 在 GitHub 创建仓库（通过网页）

# 3. 推送代码
git remote add origin https://github.com/leefly17-prog/loom-vanguard.git
git branch -M main
git push -u origin main

# 4. 创建 tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# 5. 等待 JitPack 自动构建（约 1-2 分钟）
# 访问 https://jitpack.io/#leefly17-prog/loom-vanguard 查看构建状态
```

完成！其他项目就可以通过 JitPack 引入你的库了。

