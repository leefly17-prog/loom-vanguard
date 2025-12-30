# 如何查看 GitHub 用户名

## 方法一：通过 GitHub 网站（最简单）

1. 访问 https://github.com
2. 登录你的账号
3. 点击右上角的头像
4. 你的用户名会显示在头像下方，或者直接看浏览器地址栏：
   - 访问 `https://github.com/你的用户名` 就能看到

## 方法二：通过命令行

如果你已经配置过 Git，可以查看：

```bash
# 查看 Git 配置的用户名（可能是你的名字，不是 GitHub 用户名）
git config user.name

# 查看 Git 配置的邮箱
git config user.email
```

## 方法三：查看已克隆的仓库

如果你之前克隆过其他仓库，可以查看：

```bash
# 查看远程仓库地址
cd 某个你克隆的仓库
git remote -v

# 输出类似：
# origin  https://github.com/你的用户名/仓库名.git
```

## 方法四：查看 GitHub 个人资料

1. 访问 https://github.com/settings/profile
2. 在 "Public profile" 部分可以看到你的用户名（Username）

## 方法五：通过 API

```bash
# 如果你已经配置了 GitHub token
curl https://api.github.com/user
```

## 常见情况

- **用户名**：通常是你在注册时设置的，例如 `yifei`、`zhangsan` 等
- **显示名称**：可能是你的真实姓名，这个可以改，但用户名不能改
- **URL**：你的 GitHub 主页是 `https://github.com/你的用户名`

## 示例

如果你的 GitHub 主页是：
- `https://github.com/yifei` → 用户名是 `yifei`
- `https://github.com/zhangsan` → 用户名是 `zhangsan`

---

**找到用户名后，运行替换脚本：**
```bash
./replace_username.sh 你的GitHub用户名
```

