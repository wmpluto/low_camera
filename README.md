# LowCamera 📸

### 💡 为什么写这个 App？ / Why this app?

**[中文]**
说来也巧，平时报销贴票真是个体力活。每次用手机自带相机拍发票，一张照片动辄 5MB、10MB，画质是高清得能看清纸张纤维了，但往报销系统里上传的时候简直是折磨——转圈转到怀疑人生。

其实仔细想想，报销发票这种东西，只要字迹清晰、金额能看清就行了，真的不需要那种 4K 级的艺术画质。于是我就随手写了这个 **LowCamera**。

它的核心逻辑很简单：**“够用就好”**。拍照后会自动帮你把照片缩小、压低画质，并按照“日期_时间_起点_终点_金额”这种直观的方式自动命名。这样拍出来的照片只有几百 KB，上传秒传，而且文件名直接就是报销信息，整理起来简直不要太爽！

---

**[English]**
To be honest, dealing with expense reimbursements is always a headache. Every time I used my phone's default camera to snap a picture of a receipt, the file ended up being 5MB or even 10MB. Sure, the quality was amazing—you could practically see the texture of the paper—but uploading them to the company system was a nightmare. I’d spend ages watching that little loading spinner.

The truth is, for a reimbursement receipt, you just need the text and the amount to be legible. You don't need a 4K masterpiece. So, I put together **LowCamera**.

The philosophy is simple: **"Good enough is perfect."** It automatically downsizes the image and lowers the quality right after you take the shot. Plus, it auto-names the file using a clear `Date_Time_From_To_Amount` format. The result? Files are only a few hundred KB, uploads are instant, and the filenames themselves tell you exactly what the receipt is for. Total life-saver for office work!

---

### ✨ 主要功能 / Key Features
*   **低功耗/省空间**：自动缩放和压缩图片，拒绝大文件。 (Auto-downsampling & compression)
*   **聪明命名**：拍照前填好地址和金额，文件名直接生成。 (Smart file naming with route and amount)
*   **原生预览**：内置简单的相册管理，支持大图预览、重命名和删除。 (Built-in gallery for preview, rename, and delete)
*   **快速选择**：支持日历和下拉框双重日期选择。 (Quick date selection via calendar or dropdowns)

---

### 🚀 如何开始 / Getting Started
这就是一个简单的 Android 项目，克隆下来用 Android Studio 打开就能跑。
Just a simple Android project. Clone it, open with Android Studio, and you're good to go!

---

*希望这个小工具也能帮你节省一点“转圈圈”的时间！*
*Hope this little tool saves you some "loading" time!*
