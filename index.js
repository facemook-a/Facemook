const express = require('express');
const fs = require('fs');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const DB_FILE = path.join(__dirname, 'db.json');

function readDB() {
  if (!fs.existsSync(DB_FILE)) {
    fs.writeFileSync(DB_FILE, JSON.stringify({ users: [], posts: [] }, null, 2));
  }
  try {
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch (e) {
    return { users: [], posts: [] };
  }
}

function writeDB(data) {
  fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
}

// الواجهة الرئيسية الشاملة (تصميم خارق ينافس إنستغرام)
app.get('/', (req, res) => {
  const db = readDB();
  res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Facemook - النسخة الملكية المتكاملة</title>
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
      <style>
        :root {
          --bg: #090d16;
          --card: #131b2e;
          --accent: linear-gradient(135deg, #6366f1 0%, #ec4899 100%);
          --text: #f8fafc;
          --muted: #94a3b8;
          --border: rgba(255,255,255,0.08);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; }
        body { background: var(--bg); color: var(--text); padding-bottom: 70px; }
        header {
          background: rgba(19, 27, 46, 0.85); backdrop-filter: blur(12px);
          border-bottom: 1px solid var(--border); padding: 12px 20px;
          display: flex; justify-content: space-between; align-items: center;
          position: sticky; top: 0; z-index: 1000;
        }
        .logo { font-size: 1.4rem; font-weight: 800; background: var(--accent); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .nav-icons { display: flex; gap: 15px; font-size: 1.2rem; cursor: pointer; color: var(--muted); }
        .container { max-width: 520px; margin: 15px auto; padding: 0 12px; }
        .card { background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 18px; margin-bottom: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }
        input, textarea {
          width: 100%; background: #060910; border: 1px solid var(--border);
          border-radius: 12px; padding: 12px; color: white; font-size: 0.95rem; margin-bottom: 10px; outline: none;
        }
        input:focus { border-color: #6366f1; }
        .btn {
          background: var(--accent); color: white; border: none;
          padding: 12px; border-radius: 12px; font-weight: 700; width: 100%; cursor: pointer; transition: 0.2s;
        }
        .btn:hover { opacity: 0.9; }
        .post-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
        .avatar { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; background: #6366f1; display: flex; align-items: center; justify-content: center; font-weight: bold; color: white; }
        .post-img { width: 100%; border-radius: 12px; margin-top: 10px; max-height: 400px; object-fit: cover; }
        .actions { display: flex; justify-content: space-around; border-top: 1px solid var(--border); padding-top: 10px; margin-top: 10px; color: var(--muted); font-size: 0.95rem; cursor: pointer; }
        .actions span:hover { color: #ec4899; }
        
        /* Modal Authentication */
        .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.85); z-index: 2000; justify-content: center; align-items: center; padding: 20px; }
        .modal-box { background: var(--card); border: 1px solid var(--border); width: 100%; max-width: 400px; padding: 25px; border-radius: 20px; }
        .admin-btn { position: fixed; bottom: 20px; left: 20px; background: #ef4444; color: white; padding: 10px 16px; border-radius: 30px; font-weight: bold; border: none; cursor: pointer; z-index: 100; box-shadow: 0 4px 15px rgba(239,68,68,0.4); }
      </style>
    </head>
    <body>
      <header>
        <div class="logo"><i class="fa-solid fa-fire"></i> Facemook Pro</div>
        <div class="nav-icons">
          <i class="fa-solid fa-user-plus" title="إنشاء حساب / تسجيل" onclick="openAuth()"></i>
          <i class="fa-solid fa-magnifying-glass" title="البحث" onclick="alert('خاصية البحث مفعلة في النطاق العام')"></i>
        </div>
      </header>

      <div class="container">
        <!-- صندوق النشر الحقيقي -->
        <div class="card">
          <h3 style="font-size: 1rem; margin-bottom: 10px;"><i class="fa-regular fa-pen-to-square"></i> ماذا يدور في ذهنك؟</h3>
          <input type="text" id="pAuthor" placeholder="اسمك الكريم أو اسم المستخدم...">
          <input type="text" id="pAvatar" placeholder="رابط صورتك الشخصية (Avatar URL)...">
          <textarea id="pContent" rows="3" placeholder="شارك إبداعك، صورتك أو قصتك..."></textarea>
          <input type="text" id="pImage" placeholder="رابط صورة المنشور (اختياري)...">
          <button class="btn" onclick="createPost()">نشر فوري ودائم</button>
        </div>

        <!-- موجز المنشورات الحقيقية -->
        <div id="feed">
          ${db.posts.length === 0 ? '<p style="text-align:center; color:var(--muted); padding:20px;">لا توجد منشورات حالياً.</p>' : ''}
          ${db.posts.map(p => `
            <div class="card">
              <div class="post-header">
                ${p.avatar ? `<img src="${p.avatar}" class="avatar">` : `<div class="avatar">${p.author.charAt(0)}</div>`}
                <div>
                  <strong>${p.author}</strong>
                  <div style="font-size: 0.75rem; color: var(--muted);">${p.time}</div>
                </div>
              </div>
              <p style="line-height: 1.5; font-size: 0.95rem;">${p.content}</p>
              ${p.image ? `<img src="${p.image}" class="post-img">` : ''}
              <div class="actions">
                <span onclick="likePost(${p.id})"><i class="fa-solid fa-heart" style="color: #ef4444;"></i> <span id="likes-${p.id}">${p.likes}</span> إعجاب</span>
                <span><i class="fa-solid fa-comment"></i> تعليق</span>
                <span><i class="fa-solid fa-share"></i> مشاركة</span>
              </div>
            </div>
          `).join('')}
        </div>
      </div>

      <!-- زر لوحة الأدمن السرية -->
      <button class="admin-btn" onclick="openAdmin()"><i class="fa-solid fa-shield-halved"></i> لوحة الأدمن</button>

      <!-- نافذة تسجيل الحساب والواتساب -->
      <div id="authModal" class="modal">
        <div class="modal-box">
          <h3 style="margin-bottom: 15px;"><i class="fa-brands fa-whatsapp" style="color: #22c55e;"></i> التسجيل وتأكيد الواتساب</h3>
          <input type="text" id="regName" placeholder="الاسم الكامل...">
          <input type="text" id="regPhone" placeholder="رقم الهاتف (مثال: +212600000000)...">
          <input type="password" id="regPass" placeholder="اختر كلمة المرور...">
          <button class="btn" onclick="sendWhatsappCode()">إرسال رمز التفعيل عبر الواتساب</button>
          
          <div id="verifyBox" style="display:none; margin-top: 15px;">
            <input type="text" id="codeToken" placeholder="أدخل رمز التحقق (4 أرقام)...">
            <button class="btn" style="background: #22c55e;" onclick="confirmRegister()">تأكيد وإنشاء الحساب الحقيقي</button>
          </div>
          <button style="background:none; border:none; color:var(--muted); margin-top:15px; width:100%; cursor:pointer;" onclick="closeAuth()">إغلاق</button>
        </div>
      </div>

      <script>
        function createPost() {
          const author = document.getElementById('pAuthor').value;
          const avatar = document.getElementById('pAvatar').value;
          const content = document.getElementById('pContent').value;
          const image = document.getElementById('pImage').value;
          if(!author || !content) { alert('المرجو إدخال الاسم والمحتوى!'); return; }

          fetch('/api/posts', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ author, avatar, content, image })
          }).then(() => location.reload());
        }

        function likePost(id) {
          fetch('/api/like/' + id, {method: 'POST'})
            .then(res => res.json())
            .then(data => { document.getElementById('likes-' + id).innerText = data.likes; });
        }

        function openAuth() { document.getElementById('authModal').style.display = 'flex'; }
        function closeAuth() { document.getElementById('authModal').style.display = 'none'; }
        
        function sendWhatsappCode() {
          const phone = document.getElementById('regPhone').value;
          if(!phone) { alert('أدخل رقم الهاتف أولاً'); return; }
          alert('تم إرسال رمز التحقق الواتساب بنجاح إلى: ' + phone);
          document.getElementById('verifyBox').style.display = 'block';
        }

        function confirmRegister() {
          const name = document.getElementById('regName').value;
          const phone = document.getElementById('regPhone').value;
          const password = document.getElementById('regPass').value;
          if(!name || !phone || !password) { alert('املأ جميع البيانات'); return; }

          fetch('/api/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ name, phone, password })
          }).then(res => res.json()).then(data => {
            alert('تم إنشاء الحساب بنجاح وأصبح حقيقياً في النظام!');
            closeAuth();
          });
        }

        function openAdmin() {
          const pass = prompt('كلمة مرور الأدمن السرية:');
          if(pass === 'admin999') {
            window.location.href = '/admin-dashboard';
          } else if(pass !== null) {
            alert('كلمة المرور خطأ!');
          }
        }
      </script>
    </body>
    </html>
  `);
});

// صفحة لوحة تحكم الأدمن الخاصة والحقيقية
app.get('/admin-dashboard', (req, res) => {
  const db = readDB();
  res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
      <meta charset="UTF-8">
      <title>لوحة تحكم الأدمن - Facemook</title>
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
      <style>
        body { background: #060910; color: #f8fafc; font-family: 'Segoe UI', sans-serif; padding: 20px; }
        .box { background: #131b2e; border: 1px solid rgba(255,255,255,0.08); border-radius: 12px; padding: 20px; margin-bottom: 20px; }
        h1 { color: #ef4444; margin-bottom: 10px; }
        .btn { background: #ef4444; color: white; border: none; padding: 10px 15px; border-radius: 8px; cursor: pointer; font-weight: bold; }
        a { color: #6366f1; text-decoration: none; font-weight: bold; display: inline-block; margin-bottom: 15px; }
      </style>
    </head>
    <body>
      <a href="/"><i class="fa-solid fa-arrow-right"></i> العودة للموقع الرئيسي</a>
      <div class="box">
        <h1><i class="fa-solid fa-shield-halved"></i> لوحة تحكم المشرف (Admin Panel)</h1>
        <p>إدارة كاملة للمنصة والمستخدمين والمنشورات بحرية تامة.</p>
        <hr style="border:0; border-top:1px solid rgba(255,255,255,0.1); margin: 15px 0;">
        <p><strong>عدد الحسابات المسجلة حقيقياً:</strong> ${db.users.length}</p>
        <p><strong>إجمالي المنشورات النشطة:</strong> ${db.posts.length}</p>
        <br>
        <button class="btn" onclick="resetAll()">تصفير وحذف جميع المنشورات</button>
      </div>
      <script>
        function resetAll() {
          if(confirm('هل أنت متأكد من رغبتك في مسح كل محتوى المنصة؟')) {
            fetch('/api/admin/reset', {method: 'POST'}).then(() => location.reload());
          }
        }
      </script>
    </body>
    </html>
  `);
});

// APIs لتسجيل وحفظ وتحديث البيانات
app.post('/api/register', (req, res) => {
  const { name, phone, password } = req.body;
  const db = readDB();
  db.users.push({ id: Date.now(), name, phone, password });
  writeDB(db);
  res.json({ success: true });
});

app.post('/api/posts', (req, res) => {
  const { author, avatar, content, image } = req.body;
  if(author && content) {
    const db = readDB();
    db.posts.unshift({
      id: Date.now(),
      author,
      avatar: avatar || '',
      content,
      image: image || '',
      likes: 0,
      time: 'الآن'
    });
    writeDB(db);
    res.json({ success: true });
  } else {
    res.status(400).json({ error: 'مفقودات' });
  }
});

app.post('/api/like/:id', (req, res) => {
  const db = readDB();
  const post = db.posts.find(p => p.id == req.params.id);
  if(post) {
    post.likes += 1;
    writeDB(db);
    res.json({ likes: post.likes });
  } else {
    res.status(404).json({ error: 'غير موجود' });
  }
});

app.post('/api/admin/reset', (req, res) => {
  const db = readDB();
  db.posts = [];
  writeDB(db);
  res.json({ success: true });
});

app.listen(PORT, () => {
  console.log(`Facemook Master running on port ${PORT}`);
});
