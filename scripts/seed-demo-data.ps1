param(
    [string]$Base = 'http://localhost:8080',
    [string]$Username = 'demo',
    [string]$Password = 'demo123456'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-Api($method, $url, $body, $token) {
    $headers = @{ 'Content-Type' = 'application/json; charset=utf-8' }
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    if ($null -eq $body) {
        return Invoke-RestMethod -Uri "$Base$url" -Method $method -Headers $headers -TimeoutSec 60
    }
    $json = $body | ConvertTo-Json -Depth 10
    return Invoke-RestMethod -Uri "$Base$url" -Method $method -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 120
}

function Ensure-Account {
    try {
        $reg = Invoke-Api 'POST' '/api/auth/register' @{ username = $Username; password = $Password; nickname = '演示家庭' } $null
        Write-Host "[账号] 已注册 $Username"
        return $reg.data.accessToken
    } catch {
        $login = Invoke-Api 'POST' '/api/auth/login' @{ username = $Username; password = $Password } $null
        Write-Host "[账号] 已登录 $Username"
        return $login.data.accessToken
    }
}

$token = Ensure-Account

$existing = Invoke-Api 'GET' '/api/members' $null $token
$father = $existing.data | Where-Object { $_.name -eq '张建国' } | Select-Object -First 1
if (-not $father) {
    $father = (Invoke-Api 'POST' '/api/members' @{
        name = '张建国'; relation = 'FATHER'; gender = 1; birthDate = '1967-03-15'
        heightCm = 172.0; weightKg = 80.5; bloodType = 'A'; phone = '13800001234'
        remark = '有高血压病史，年度体检刚做完'
    } $token).data
    Write-Host "[成员] 创建 张建国 id=$($father.id)"
} else {
    Write-Host "[成员] 已存在 张建国 id=$($father.id)"
}
$memberId = $father.id

$daughter = $existing.data | Where-Object { $_.name -eq '张小雨' } | Select-Object -First 1
if (-not $daughter) {
    $daughter = (Invoke-Api 'POST' '/api/members' @{
        name = '张小雨'; relation = 'CHILD'; gender = 2; birthDate = '2014-09-01'
        heightCm = 152.0; weightKg = 41.0; bloodType = 'O'; remark = '小学六年级'
    } $token).data
    Write-Host "[成员] 创建 张小雨 id=$($daughter.id)"
}

$profileCreated = $false
try {
    $profile = Invoke-Api 'GET' "/api/members/$memberId/profile" $null $token
    if ($profile.data.medicalHistories.Count -gt 0) {
        $profileCreated = $true
    }
} catch {}
if (-not $profileCreated) {
    Invoke-Api 'POST' "/api/members/$memberId/medical-histories" @{ diseaseName = '高血压（2级）'; diseaseStatus = 'MANAGING'; diagnosedDate = '2018-05-01'; treatmentDesc = '口服氨氯地平 5mg，每日一次'; hospital = '市第一人民医院' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/medical-histories" @{ diseaseName = '非酒精性脂肪肝（轻度）'; diseaseStatus = 'MANAGING'; diagnosedDate = '2022-08-10'; treatmentDesc = '饮食控制 + 有氧运动，定期复查肝功能'; hospital = '市第一人民医院' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/medical-histories" @{ diseaseName = '急性阑尾炎手术'; diseaseStatus = 'CURED'; diagnosedDate = '2005-04-18'; treatmentDesc = '腹腔镜阑尾切除术' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/allergies" @{ allergen = '青霉素'; allergyType = 'DRUG'; reaction = '全身皮疹伴瘙痒'; severity = 'SEVERE'; remark = '禁用青霉素类抗生素' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/allergies" @{ allergen = '海鲜（虾蟹）'; allergyType = 'FOOD'; reaction = '口唇发麻、荨麻疹'; severity = 'MODERATE' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/medications" @{ drugName = '氨氯地平片'; dose = '5mg'; frequency = '每日 1 次'; route = 'ORAL'; startDate = '2018-05-01'; purpose = '降压'; status = 'TAKING'; doctorAdvice = '晨起服用，定期监测血压' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/medications" @{ drugName = '阿托伐他汀钙片'; dose = '20mg'; frequency = '每晚 1 次'; route = 'ORAL'; startDate = '2026-02-20'; purpose = '降脂'; status = 'TAKING'; doctorAdvice = '睡前服用，3 个月复查血脂与肝功能' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/medications" @{ drugName = '维生素 D3'; dose = '1000IU'; frequency = '每日 1 次'; route = 'ORAL'; startDate = '2026-08-20'; purpose = '补充维生素D'; status = 'TAKING' } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/family-histories" @{ relation = 'MOTHER'; diseaseName = '2型糖尿病'; onsetAge = 62 } $token | Out-Null
    Invoke-Api 'POST' "/api/members/$memberId/family-histories" @{ relation = 'FATHER'; diseaseName = '冠心病'; onsetAge = 68; remark = '68 岁行冠脉支架植入' } $token | Out-Null
    Invoke-Api 'PUT' "/api/members/$memberId/lifestyle" @{
        smoking = 'QUIT'; smokingAmount = '已戒烟 5 年，此前每日约 10 支'
        drinking = 'OCCASIONAL'; drinkingAmount = '应酬时少量白酒'
        sleepHours = 6.5; sleepQuality = 'FAIR'
        exerciseFrequency = '每周 2-3 次快走'; sedentaryHours = 9.0
        dietHabit = '口味偏咸，爱吃红烧肉，蔬菜偏少'
        stressLevel = 'MEDIUM'
    } $token | Out-Null
    Write-Host '[档案] 既往病史/过敏/用药/家族史/生活习惯 已写入'
}

$metrics = @(
    @{ id = 2001; name = '白细胞计数'; v = @('7.2', '6.8', '6.1') },
    @{ id = 2002; name = '红细胞计数'; v = @('5.1', '5.0', '4.9') },
    @{ id = 2003; name = '血红蛋白'; v = @('152', '150', '148') },
    @{ id = 2004; name = '红细胞压积'; v = @('45.8', '45.1', '44.2') },
    @{ id = 2005; name = '平均红细胞体积'; v = @('90.2', '90.5', '90.1') },
    @{ id = 2006; name = '平均血红蛋白量'; v = @('30.1', '30.3', '30.0') },
    @{ id = 2007; name = '平均血红蛋白浓度'; v = @('334', '335', '336') },
    @{ id = 2008; name = '血小板计数'; v = @('226', '218', '210') },
    @{ id = 2009; name = '中性粒细胞比率'; v = @('58.2', '60.1', '62.4') },
    @{ id = 2010; name = '淋巴细胞比率'; v = @('33.5', '31.8', '30.2') },
    @{ id = 2011; name = '单核细胞比率'; v = @('6.1', '5.8', '6.0') },
    @{ id = 2012; name = '嗜酸性粒细胞比率'; v = @('1.8', '2.0', '1.6') },
    @{ id = 2021; name = '谷丙转氨酶'; v = @('45', '56', '62') },
    @{ id = 2022; name = '谷草转氨酶'; v = @('32', '39', '45') },
    @{ id = 2023; name = '总胆红素'; v = @('14.1', '14.8', '15.2') },
    @{ id = 2024; name = '直接胆红素'; v = @('3.8', '4.0', '4.1') },
    @{ id = 2025; name = '间接胆红素'; v = @('10.3', '10.8', '11.1') },
    @{ id = 2026; name = '总蛋白'; v = @('73.5', '72.8', '72.0') },
    @{ id = 2027; name = '白蛋白'; v = @('45.2', '44.6', '44.0') },
    @{ id = 2028; name = '球蛋白'; v = @('28.3', '28.2', '28.0') },
    @{ id = 2029; name = '白球比'; v = @('1.60', '1.58', '1.57') },
    @{ id = 2030; name = '碱性磷酸酶'; v = @('82', '85', '88') },
    @{ id = 2031; name = 'γ-谷氨酰转肽酶'; v = @('62', '70', '78') },
    @{ id = 2041; name = '尿素'; v = @('5.2', '5.4', '5.6') },
    @{ id = 2042; name = '肌酐'; v = @('85', '86', '88') },
    @{ id = 2043; name = '尿酸'; v = @('428', '440', '452') },
    @{ id = 2044; name = '估算肾小球滤过率'; v = @('99', '97', '96') },
    @{ id = 2045; name = '胱抑素C'; v = @('0.88', '0.90', '0.92') },
    @{ id = 2061; name = '总胆固醇'; v = @('5.4', '5.8', '6.1') },
    @{ id = 2062; name = '甘油三酯'; v = @('1.9', '2.2', '2.4') },
    @{ id = 2063; name = '高密度脂蛋白胆固醇'; v = @('0.98', '0.94', '0.91') },
    @{ id = 2064; name = '低密度脂蛋白胆固醇'; v = @('3.5', '3.8', '4.1') },
    @{ id = 2065; name = '载脂蛋白A1'; v = @('1.18', '1.15', '1.12') },
    @{ id = 2066; name = '载脂蛋白B'; v = @('1.22', '1.29', '1.35') },
    @{ id = 2081; name = '空腹血糖'; v = @('5.9', '6.2', '6.6') },
    @{ id = 2082; name = '糖化血红蛋白'; v = @('5.9', '6.1', '6.4') },
    @{ id = 2083; name = '空腹胰岛素'; v = @('14.6', '16.3', '18.2') },
    @{ id = 2101; name = '促甲状腺激素'; v = @('2.8', '3.0', '3.1') },
    @{ id = 2102; name = '游离三碘甲状腺原氨酸'; v = @('4.6', '4.5', '4.5') },
    @{ id = 2103; name = '游离甲状腺素'; v = @('16.8', '16.5', '16.2') },
    @{ id = 2104; name = '总三碘甲状腺原氨酸'; v = @('2.0', '1.95', '1.9') },
    @{ id = 2105; name = '总甲状腺素'; v = @('108', '106', '105') },
    @{ id = 2106; name = '甲状腺过氧化物酶抗体'; v = @('10', '11', '12') },
    @{ id = 2121; name = '尿蛋白'; v = @('阴性', '阴性', '阴性') },
    @{ id = 2122; name = '尿糖'; v = @('阴性', '阴性', '阴性') },
    @{ id = 2123; name = '尿潜血'; v = @('阴性', '阴性', '阴性') },
    @{ id = 2124; name = '尿白细胞'; v = @('阴性', '阴性', '阴性') },
    @{ id = 2125; name = '尿比重'; v = @('1.022', '1.019', '1.020') },
    @{ id = 2126; name = '尿酸碱度'; v = @('6.5', '6.2', '6.0') },
    @{ id = 2127; name = '尿酮体'; v = @('阴性', '阴性', '阴性') },
    @{ id = 2141; name = '血钾'; v = @('4.3', '4.2', '4.2') },
    @{ id = 2142; name = '血钠'; v = @('142', '141', '141') },
    @{ id = 2143; name = '血氯'; v = @('104', '103', '103') },
    @{ id = 2144; name = '血钙'; v = @('2.35', '2.33', '2.32') },
    @{ id = 2145; name = '血磷'; v = @('1.15', '1.13', '1.12') },
    @{ id = 2146; name = '血镁'; v = @('0.90', '0.89', '0.88') },
    @{ id = 2161; name = '肌酸激酶'; v = @('118', '126', '132') },
    @{ id = 2162; name = '肌酸激酶同工酶'; v = @('10', '11', '12') },
    @{ id = 2163; name = '乳酸脱氢酶'; v = @('172', '178', '185') },
    @{ id = 2164; name = '同型半胱氨酸'; v = @('16.2', '17.4', '18.6') },
    @{ id = 2181; name = '甲胎蛋白'; v = @('2.8', '3.0', '3.2') },
    @{ id = 2182; name = '癌胚抗原'; v = @('1.9', '2.0', '2.1') },
    @{ id = 2183; name = '糖类抗原19-9'; v = @('11.2', '11.8', '12.4') },
    @{ id = 2184; name = '前列腺特异性抗原'; v = @('1.6', '1.7', '1.8') },
    @{ id = 2201; name = 'C反应蛋白'; v = @('2.8', '3.0', '3.2') },
    @{ id = 2202; name = '超敏C反应蛋白'; v = @('2.9', '3.2', '3.6') },
    @{ id = 2203; name = '降钙素原'; v = @('0.02', '0.02', '0.02') },
    @{ id = 2221; name = '25-羟基维生素D'; v = @('19.8', '18.2', '16.5') },
    @{ id = 2222; name = '铁蛋白'; v = @('196', '203', '210') },
    @{ id = 2223; name = '叶酸'; v = @('7.6', '7.2', '6.8') },
    @{ id = 2224; name = '维生素B12'; v = @('356', '338', '320') }
)

$reportDates = @('2026-02-20', '2026-05-20', '2026-08-20')
$reportNames = @('2026年2月体检', '2026年5月复查', '2026年8月年度体检')
$existingReports = (Invoke-Api 'GET' "/api/reports?memberId=$memberId&pageSize=50" $null $token).data.list
for ($r = 0; $r -lt $reportDates.Count; $r++) {
    $date = $reportDates[$r]
    $already = $existingReports | Where-Object { $_.reportDate -eq $date } | Select-Object -First 1
    if ($already) {
        Write-Host "[报告] $date 已存在，跳过"
        continue
    }
    $indicators = @()
    $sort = 0
    foreach ($m in $metrics) {
        $sort++
        $indicators += @{
            metricId = $m.id
            metricName = $m.name
            valueText = $m.v[$r]
            sort = $sort
        }
    }
    Invoke-Api 'POST' '/api/reports/manual' @{
        memberId = [long]$memberId
        reportName = $reportNames[$r]
        reportDate = $date
        institution = '市第一人民医院体检中心'
        reportType = 'ANNUAL'
        remark = '演示数据：包含全部 ' + $metrics.Count + ' 项指标'
        indicators = $indicators
    } $token | Out-Null
    Write-Host "[报告] $date 已创建（$($metrics.Count) 项指标）"
}

$dailyExisting = (Invoke-Api 'GET' "/api/members/$memberId/exercise/daily/range" $null $token).data
if ($dailyExisting.Count -lt 10) {
    $today = Get-Date
    for ($i = 59; $i -ge 0; $i--) {
        $day = $today.AddDays(-$i)
        $dow = [int]$day.DayOfWeek
        $baseSteps = 5200 + (Get-Random -Minimum -1500 -Maximum 4500)
        if ($dow -eq 0 -or $dow -eq 6) { $baseSteps += 4000 }
        if ($i % 9 -eq 0) { $baseSteps = 3200 + (Get-Random -Minimum 0 -Maximum 1500) }
        $active = [int]($baseSteps / 110)
        $calories = [math]::Round($baseSteps * 0.04 + (Get-Random -Minimum 20 -Maximum 90), 1)
        $weight = [math]::Round(80.5 - (59 - $i) * 0.035 + (Get-Random -Minimum -30 -Maximum 30) / 100.0, 1)
        $record = @{
            recordDate = $day.ToString('yyyy-MM-dd')
            steps = $baseSteps
            activeMinutes = $active
            caloriesKcal = $calories
            sleepHours = [math]::Round(5.8 + (Get-Random -Minimum 0 -Maximum 200) / 100.0, 1)
            sleepQuality = @('GOOD', 'FAIR', 'POOR')[(Get-Random -Minimum 0 -Maximum 3)]
            weightKg = $weight
        }
        if ($i -lt 14) {
            $record['systolicBp'] = 128 + (Get-Random -Minimum 0 -Maximum 18)
            $record['diastolicBp'] = 82 + (Get-Random -Minimum 0 -Maximum 10)
            $record['bloodGlucose'] = [math]::Round(5.8 + (Get-Random -Minimum 0 -Maximum 120) / 100.0, 1)
            $record['glucoseType'] = 'FASTING'
            $record['mood'] = @('GOOD', 'FAIR', 'GOOD')[(Get-Random -Minimum 0 -Maximum 3)]
        }
        Invoke-Api 'PUT' "/api/members/$memberId/exercise/daily" $record $token | Out-Null
    }
    Write-Host '[运动] 近 60 天日常记录已写入'
}

$workoutExisting = (Invoke-Api 'GET' "/api/members/$memberId/exercise/workouts?pageSize=50" $null $token).data.total
if ($workoutExisting -eq 0) {
    $workoutTemplates = @(
        @{ type = 'WALKING'; title = '晚饭后快走'; min = 35; intensity = 'MEDIUM'; cal = 150; km = 3.2 },
        @{ type = 'WALKING'; title = '公园快走'; min = 45; intensity = 'MEDIUM'; cal = 195; km = 4.3 },
        @{ type = 'RUNNING'; title = '慢跑 4 公里'; min = 30; intensity = 'HIGH'; cal = 310; km = 4.0; hr = 152 },
        @{ type = 'CYCLING'; title = '周末骑行'; min = 65; intensity = 'MEDIUM'; cal = 420; km = 16.5 },
        @{ type = 'SWIMMING'; title = '蛙泳 800 米'; min = 40; intensity = 'MEDIUM'; cal = 330; km = 0.8 },
        @{ type = 'STRENGTH'; title = '哑铃力量训练'; min = 30; intensity = 'MEDIUM'; cal = 180 },
        @{ type = 'YOGA'; title = '睡前拉伸瑜伽'; min = 20; intensity = 'LOW'; cal = 70 }
    )
    $today = Get-Date
    for ($i = 0; $i -lt 18; $i++) {
        $template = $workoutTemplates[$i % $workoutTemplates.Count]
        $day = $today.AddDays(-3 * $i - (Get-Random -Minimum 0 -Maximum 2))
        $record = @{
            workoutDate = $day.ToString('yyyy-MM-dd')
            workoutType = $template.type
            title = $template.title
            durationMin = $template.min + (Get-Random -Minimum -5 -Maximum 6)
            intensity = $template.intensity
            caloriesKcal = [math]::Round($template.cal + (Get-Random -Minimum -30 -Maximum 31), 0)
            feeling = @('EASY', 'MODERATE', 'HARD')[(Get-Random -Minimum 0 -Maximum 3)]
        }
        if ($template.km) { $record['distanceKm'] = $template.km }
        if ($template.hr) { $record['avgHeartRate'] = $template.hr }
        Invoke-Api 'POST' "/api/members/$memberId/exercise/workouts" $record $token | Out-Null
    }
    Write-Host '[运动] 18 条训练记录已写入'
}

Write-Host ''
Write-Host '===== 演示数据就绪 ====='
Write-Host "账号: $Username  密码: $Password"
Write-Host '成员1: 张建国（全部 71 项指标 x 3 次报告、60 天运动、完整档案）'
Write-Host '成员2: 张小雨（基础档案）'
Write-Host "登录地址: http://localhost:5173"
