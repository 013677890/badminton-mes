Add-Type -AssemblyName System.Drawing
$w=2400; $h=1500
$bmp=New-Object Drawing.Bitmap($w,$h)
$g=[Drawing.Graphics]::FromImage($bmp); $g.SmoothingMode='AntiAlias'; $g.Clear([Drawing.Color]::White)
$blue=[Drawing.Color]::FromArgb(234,246,255); $green=[Drawing.Color]::FromArgb(221,244,231); $purple=[Drawing.Color]::FromArgb(240,232,255); $red=[Drawing.Color]::FromArgb(255,229,229); $yellow=[Drawing.Color]::FromArgb(255,241,214); $line=[Drawing.Color]::FromArgb(22,131,184)
$pen=New-Object Drawing.Pen($line,2); $font=New-Object Drawing.Font('Microsoft YaHei',14,[Drawing.FontStyle]::Bold); $title=New-Object Drawing.Font('Microsoft YaHei',24,[Drawing.FontStyle]::Bold); $small=New-Object Drawing.Font('Microsoft YaHei',11)
function DrawT($s,$x,$y,$ww,$hh,$f=$font){$sz=$g.MeasureString($s,$f);$g.DrawString($s,$f,[Drawing.Brushes]::Black,[single]($x+($ww-$sz.Width)/2),[single]($y+($hh-$sz.Height)/2))}
function DrawE($name,$x,$y,$c=$blue){$g.FillRectangle((New-Object Drawing.SolidBrush($c)),$x,$y,180,60);$g.DrawRectangle($pen,$x,$y,180,60);DrawT $name $x $y 180 60}
function DrawR($name,$x,$y){$p=@([Drawing.Point]::new($x+60,$y),[Drawing.Point]::new($x+120,$y+28),[Drawing.Point]::new($x+60,$y+56),[Drawing.Point]::new($x,$y+28));$g.FillPolygon((New-Object Drawing.SolidBrush([Drawing.Color]::FromArgb(217,238,255))),$p);$g.DrawPolygon($pen,$p);DrawT $name $x $y 120 56 $small}
function DrawL($x1,$y1,$x2,$y2,$lab){$g.DrawLine($pen,$x1,$y1,$x2,$y2)}
DrawT '羽毛球制造 MES 系统总体 E-R 图' 700 25 1000 50 $title; DrawT '总体概念模型：跨模块核心实体与关系' 850 80 700 30 $small
DrawE '用户' 70 170; DrawE '角色' 70 320; DrawR '授权' 100 245; DrawL 160 230 160 245 '1'; DrawL 160 301 160 320 'n'
DrawE '客户' 340 170; DrawE '产品' 600 170; DrawE '物料' 860 170; DrawE 'BOM' 860 320; DrawR '订购' 470 245; DrawR '组成' 790 245; DrawL 510 200 600 200 'n'; DrawL 770 200 850 200 'n'; DrawL 690 230 850 320 '1'
DrawE '工艺路线' 1110 170; DrawE '工序' 1110 320; DrawR '定义' 1020 245; DrawR '包含' 1140 405; DrawL 770 200 1020 200 '1'; DrawL 1080 273 1110 320 'n'
DrawE '车间' 340 480; DrawE '产线' 600 480; DrawE '工位' 860 480; DrawR '包含' 430 405; DrawR '包含' 690 405; DrawL 430 480 430 433 '1'; DrawL 550 510 600 510 'n'; DrawL 690 480 750 433 '1'; DrawL 810 510 860 510 'n'
DrawE '生产订单' 1370 170 $green; DrawE '生产工单' 1630 170 $green; DrawE '生产任务' 1890 170 $green; DrawR '生成' 1570 245; DrawR '分解' 1830 245; DrawL 1370 200 1570 273 '1'; DrawL 1690 273 1890 200 'n'; DrawL 1810 200 1830 273 '1'; DrawL 1950 273 1980 230 'n'
DrawE '设备' 1110 600 $yellow; DrawR '配置' 950 520; DrawL 950 508 1110 630 'n'
DrawE '条码/批次' 1370 450 $purple; DrawE '库存记录' 1630 450 $purple; DrawE '质量检验' 1890 450 $red; DrawR '入库/出库' 1660 535; DrawR '检验' 1830 535; DrawL 1460 510 1660 563 '1'; DrawL 1780 563 1810 510 'n'; DrawL 1790 510 1830 563 '1'; DrawL 1950 563 1980 510 'n'
DrawE '安灯事件' 1370 720 $red; DrawE '计件工资' 1630 720 $yellow; DrawR '触发' 1430 650; DrawR '计件' 1620 650; DrawL 1200 660 1430 678 '1'; DrawL 1550 678 1630 720 'n'; DrawL 1980 230 1680 720 '1'
$bmp.Save('Picture\MES总体E-R图.png',[Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()
