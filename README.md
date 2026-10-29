# XPlayer - Video Player All Format (REA Reconstructed Clone)

هذا المشروع تم استخراجه وتحليله وإعادة بنائه بالكامل باستخدام أداة الهندسة العكسية **REA (Reverse Engineer Anything)** من حزمة تطبيق **XPlayer**:
`C:\Users\Ahmed Bakr\Desktop\XPlayer - Video Player.apk`

---

## 📂 هيكلية المشروع (Project Structure)

```
xplayer-clone/
├── ARCHITECTURE.md          # تقرير تحليلي شامل للهندسة العكسية ومكونات التطبيق (Activities, Services, Permissions)
├── README.md                # دليل الاستخدام والتشغيل
├── decompiled-resources/    # الموارد المستخرجة من الـ APK بواسطة REA
│   ├── AndroidManifest.xml  # ملف المانيفست الثنائي المستخرج
│   ├── resources.arsc       # جدول الموارد الثنائية
│   ├── assets/              # ملفات الـ Assets والرسوم المتحركة (Lottie)
│   └── res/                 # الأيقونات، الصور، والتخطيطات (Drawables & Layouts)
└── app/                     # تطبيق مشغل الفيديو (Clone Application)
    ├── index.html           # واجهة المستخدم وتجربة المشغل التفاعلية
    ├── app.js               # محرك التشغيل، الإيماءات، والمعادل الصوتي
    └── assets/              # الأيقونات والصور المستخرجة من التطبيق الأصلي
```

---

## 🚀 كيفية تشغيل التطبيق (Run the Clone)

يمكنك تشغيل التطبيق بإحدى الطرق التالية:

### الطريقة 1: الفتح المباشر في المتصفح
فقط اضغط مرتين (Double Click) على الملف:
```
C:\Users\Ahmed Bakr\.gemini\antigravity\scratch\xplayer-clone\app\index.html
```
سيفتح في Google Chrome أو Edge مباشرة بدون أي متطلبات!

### الطريقة 2: تشغيل سيرفر محلي (Local HTTP Server)
إذا أردت تشغيله عبر سيرفر محلي:
```powershell
cd "C:\Users\Ahmed Bakr\.gemini\antigravity\scratch\xplayer-clone\app"
python -m http.server 3000
```
ثم افتح: `http://localhost:3000`

---

## ✨ المميزات المستنسخة من XPlayer الأصلي:

1. **التحكم بالإيماءات (Touch & Mouse Gestures):**
   - سحب عمودي على النصف الأيسر: التحكم في السطوع (Brightness).
   - سحب عمودي على النصف الأيمن: التحكم في الصوت (Volume).
   - سحب أفقي: تقديم وتأخير شريط الفيديو (Seek Scrubbing).
   - النقر المزدوج (Double Click): تخطي 10 ثوانٍ للأمام أو الخلف.
2. **الخزنة السرية (Private Vault):**
   - حماية الوسائط برمز PIN مكون من 4 أرقام (الرمز الافتراضي: `1234`).
   - إمكانية إخفاء أي فيديو من المكتبة العامة ونقله للخزنة.
3. **معادل صوتي احترافي (5-Band Audio Equalizer):**
   - نطاقات التردد: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz.
   - مضخم صوت جهير (Bass Boost 200%).
   - إعدادات مسبقة: Rock, Pop, Cinema Movie, Bass, Vocal.
4. **دعم تشغيل الملفات المحلية:**
   - تشغيل أي ملف فيديو من جهاز الكمبيوتر عبر زر "Open File" أو السحب والإفلات (Drag & Drop).
5. **تنسيقات شاشة متعددة (Aspect Ratio):**
   - Fit, Fill, 16:9, 4:3.
6. **نافذة عائمة (Picture-in-Picture) وقفل الشاشة (Screen Lock).**
7. **تحميل الترجمات المخصصة (.SRT / .VTT).**
8. **التقاط صور من الفيديو (Screenshot Capture).**
9. **وضع القراءة الليلي (Night Mode Filter) ومؤقت النوم (Sleep Timer).**
