export const SITE_CONTENT = {
  brand: {
    name: "TalexSoulTech",
    shortName: "SoulTech",
    tagline: "用可验证的系统，把生存服推进成一条可维护的工业与探索进程。",
    visualStatement: "本站采用原创方块工业视觉表达，不复用 Minecraft 官方纹理，也不依赖第三方追踪或 Cookie 横幅。",
  },
  nav: [
    { id: "overview", label: "插件概述" },
    { id: "play", label: "玩法进程" },
    { id: "install", label: "安装与排错" },
    { id: "architecture", label: "技术架构" },
    { id: "cloud", label: "云端控制台" },
  ],
  hero: {
    eyebrow: "Paper 26.1.2 · Java 25 · 原创方块工业玩法",
    title: "把生存服建成可维护的工业系统",
    description: "TalexSoulTech 一期把本地生存玩法、MySQL 玩家持久化、Paper 事件驱动机器与 Cloudflare 多租户 SaaS 接在同一条清晰的进程上。当前可用能力与后续策划在本站严格分开说明。",
    primaryAction: "从兼容矩阵开始部署",
    secondaryAction: "查看一期已实现边界",
  },
  overview: {
    title: "一期插件概述",
    lead: "一期的目标不是堆叠物品数量，而是先建立能被玩家理解、被服主维护、被后续内容扩展的基础循环：采集与整理、配方处理、能源分配、设备推进、向导解锁。",
    implemented: [
      {
        title: "八个已落地学科根目录",
        detail: "基础、材料、植物、防御、科技、魔法、空间、引力已经在分类与向导体系中出现；它们是当前内容的真实边界。",
      },
      {
        title: "生产已注册 150 个稳定物品 ID",
        detail: "当前生产 JAR 通过 /tst items 暴露 150 个唯一注册项，其中包括 47 件便携电力装备、3 台无线充电多方块和铜/铁/虚空箱。官网 /runtime 直接展示这份运行目录，不再用策划条目推测现状。",
      },
      {
        title: "38 台机器进入统一目录",
        detail: "生产运行时包含 5 台旧式机器与 33 台供电多方块；多方块继承 UUID 所有权、结构声明、电网端点、库存事务与重启恢复边界。",
      },
      {
        title: "可持久化的玩家状态",
        detail: "玩家进入时载入数据，离开或正常停服时写回。向导书、分类解锁与吸附相关状态围绕 PlayerData 组织。",
      },
      {
        title: "可受控管理的云端扩展",
        detail: "一期控制台已具备 Cordis 风格扩展 Context、依赖拓扑、LIFO 资源释放、原子热更新、last-known-good 回退、Lua/JS 沙箱、能力权限、云端增删启停与审计。扩展运行在服务器租户边界内，不直接接管 Paper 玩法线程。",
      },
    ],
    boundaries: [
      "27 学科与 810 物品是完整策划容量，不是当前生产注册数；实际运行物品以 /runtime 的 150 个稳定 ID 为准。",
      "当前 JAR 与资源包均由官网受控分发，并公开 SHA-256 清单；未知镜像、聊天附件和非清单制品不属于发行渠道。",
      "云端功能以受控配对、快照同步、服主控制台与按能力受限的扩展运行时为边界；它不替代 Paper 服内的实时玩法判定。",
    ],
  },
  compatibility: {
    title: "兼容矩阵",
    lead: "兼容结论以当前项目构建设置与插件元数据为准；未声明的服务端实现不视为支持承诺。",
    matrix: [
      {
        target: "Java 运行时",
        requirement: "Java 25",
        status: "必需",
        detail: "Maven 编译 release 为 25。低版本 JVM 不是本构建产物的兼容目标。",
      },
      {
        target: "Paper 服务端",
        requirement: "Paper 26.1.2.build.74-stable",
        status: "构建基线",
        detail: "当前 pom 依赖该 Paper API 版本；部署应以同一 Paper 26.1.2 系列作为基线。",
      },
      {
        target: "插件元数据 API",
        requirement: "api-version 1.21.3",
        status: "元数据声明",
        detail: "这是 plugin.yml 的 Paper API 元数据，不会替代 Java 25 与 Paper 26.1.2 的构建基线。",
      },
      {
        target: "数据库",
        requirement: "可从 Paper 容器或主机访问的 MySQL",
        status: "正式玩法前置条件",
        detail: "项目内置 mysql-connector-j 9.5.0；数据库服务版本和网络拓扑应由服主在受控环境中验证。",
      },
      {
        target: "其他 Bukkit 系实现",
        requirement: "未承诺",
        status: "不作为兼容目标",
        detail: "不要将 Spigot、Purpur 或未知分支的可启动结果当作官方支持结论。",
      },
    ],
  },
  download: {
    title: "下载与产物说明",
    availability: "生产构建产物分发",
    artifact: "talex-soul-tech-3.0.0-SNAPSHOT.jar",
    detail: "当前生产构件、官网下载 JAR 与发布清单保持同一 SHA-256；下载页同时提供资源包清单，服主应在部署前核对提交来源、大小与摘要。",
    steps: [
      "从官网下载页取得 JAR，并以 manifest.json 中的 SHA-256 校验完整性。",
      "将 JAR 先上传到临时路径，再以 .jar.new 原子替换目标 Paper 插件；保留旧制品作为回滚材料。",
      "确认 Java 25、Paper 26.1.2、数据库与资源包前置条件，再允许玩家进入。",
      "启动后用 /tst items、/tst machines、/tst power 与 /tst cloud status 核对运行边界。",
    ],
    integrityRule: "只有官网清单摘要匹配的 JAR 与资源包才属于当前发布。",
  },
  quickInstall: {
    title: "快速安装",
    lead: "这是一条面向服主的最短安全路径：先让本地持久化可用，再让玩家进入，再按需接入云端。",
    steps: [
      {
        step: "01",
        title: "固定运行基线",
        detail: "使用 Java 25 与 Paper 26.1.2.build.74-stable；不要用未声明的运行时替代构建基线。",
      },
      {
        step: "02",
        title: "投放受控 JAR",
        detail: "把当前构建产物放入 plugins 目录，并在变更前保存旧 JAR、plugins/TalexSoulTech 与数据库备份。",
      },
      {
        step: "03",
        title: "先接通 MySQL",
        detail: "在 config.yml 的 Settings.mysql 下配置可达的数据库，再将 Settings.mysql.enabled 设为 true。正式玩法不应以关闭持久化的状态对外开放。",
      },
      {
        step: "04",
        title: "首次启动与本地检查",
        detail: "插件会初始化分类、机器管理器、方块缓存与电网周期。确认玩家数据表与系统表已建立后，再让玩家进入。",
      },
      {
        step: "05",
        title: "用真实玩家路径验收",
        detail: "首次进入的玩家会获得向导书；使用 /tst 打开命令帮助，按向导书进入分类与机器，而不是直接发放未知 NBT 物品。",
      },
      {
        step: "06",
        title: "按需配对云端",
        detail: "cloud.enabled 默认应保持 false。服主在控制台生成一次性配对码后，才使用 /tst cloud link 配对码；命令和界面都不回显 API Key。",
      },
    ],
  },
  mysql: {
    title: "MySQL 玩家持久化",
    lead: "MySQL 不是一期正式服的可选装饰。PlayerData 在玩家进入时读取 soul_tech_player_data，在离开时写回状态；关闭启动连接并不等同于获得一个可安全运营的无持久化玩法模式。",
    currentTables: [
      {
        table: "soul_tech_player_data",
        purpose: "存储玩家 UUID、名称与 Base64 编码的玩家 JSON 状态。",
      },
      {
        table: "soul_tech_system",
        purpose: "提供插件系统键值数据的建表入口。",
      },
    ],
    configuration: [
      { key: "Settings.mysql.enabled", meaning: "正式服设为 true；默认 false 仅用于尚未配置数据库的启动前状态。" },
      { key: "Settings.mysql.ip 与 port", meaning: "填写 Paper 运行环境可以访问的私网数据库地址与端口。" },
      { key: "Settings.mysql.db", meaning: "填写为本插件隔离的数据库名称。" },
      { key: "Settings.mysql.user 与 pass", meaning: "使用只授予该数据库所需权限的专用账户；不把凭据写入仓库、站点或命令输出。" },
    ],
    safeguards: [
      "连接串包含 autoReconnect、Asia/Shanghai 时区、受控网络中的 SSL 选择以及 allowPublicKeyRetrieval。后者只适合受控私网，不是公网 TLS 的替代品。",
      "为 Minecraft 实际容器网段建立最小权限账号，只授予本库所需的 SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER、INDEX；不要授予全局权限或 DROP。",
      "正常停服会遍历在线 PlayerData 执行 leave，再关闭数据库连接；异常断电仍需要数据库备份与服主恢复流程。",
    ],
  },
  guidebook: {
    title: "向导书：玩家的第一张系统地图",
    lead: "新玩家没有安装向导书时，PlayerData 初始化会发放一本带 guide 类型标记的灵魂科技向导书。交互监听器识别该标记后，按首次引导、上次阅读位置或完整向导书分流打开界面。",
    useFlow: [
      "首次进入：获得向导书，阅读基础与材料线，确认下一项可做的配方或分类。",
      "再次打开：优先恢复上次向导位置；没有历史位置时进入完整向导目录。",
      "分类推进：CategoryObject 用父节点、子节点与前置分类描述学习关系；配方对象会回指自己的分类条目。",
      "服主支持：不要通过修改物品显示名模拟向导书，使用系统生成的物品与原有事件入口。",
    ],
    playerPromise: "向导书负责解释进程，不暗示未实装学科已经可以解锁。",
  },
  machines: {
    title: "机器：统一注册，按事件打开",
    lead: "BaseMachine 构造时自动向 MachineManager 注册机器名称、展示物品与 MachineChecker。玩家交互先经过保护检查，再由 MachineManager 找到首个匹配检查器并打开对应机器界面。",
    currentTypes: [
      { name: "高级工作台", code: "AdvancedWorkBench", role: "高级配方的工作台入口。", state: "已实现" },
      { name: "破碎锤机器", code: "BreakHammerMachine", role: "破碎处理流程的机器入口。", state: "已实现" },
      { name: "压缩机", code: "Compressor", role: "压缩系列的机器入口。", state: "已实现" },
      { name: "炉釜机器", code: "FurnaceCauldronMachine", role: "炉釜配方处理的机器入口。", state: "已实现" },
      { name: "烤盘机器", code: "GriddleMachine", role: "烤盘配方处理的机器入口。", state: "已实现" },
    ],
    operatingRules: [
      "机器不是独立的命令菜单；它们由 Paper 玩家交互事件、MachineChecker 与 PlayerData 共同驱动。",
      "玩家每次交互先经 ProtectorManager 校验。保护失败时不应绕过检查器强开机器。",
      "机器数据在正常禁用时序列化到缓存并在下次启动装载；这不是替代数据库和服务器级备份的理由。",
    ],
  },
  power: {
    title: "电网：先保证结算，再谈扩张",
    lead: "一期电网以端点、线缆、缓存与周期统计为最小闭环。它以服务端主线程为结算边界，用有限吞吐、明确损耗和稳定顺序避免机器规模增长后出现不可解释的能量跳变。",
    components: [
      {
        name: "EnergyBuffer",
        detail: "保存容量与当前储量；receive 与 extract 都会裁剪到可用空间或现有储量，并支持模拟计算。",
      },
      {
        name: "PowerEndpoint",
        detail: "端点声明 BlockKey、生产者/储能/消费者类型、EnergyBuffer、每周期输入输出上限、优先级与周期钩子。",
      },
      {
        name: "PowerCable",
        detail: "线缆带有每周期吞吐上限与千分比损耗；传输损耗按整数规则向上取整，避免小额传输长期漏算。",
      },
      {
        name: "PowerGrid",
        detail: "以方块位置注册端点和线缆，拓扑变更后用六向邻接重建网络；超出节点上限的网络会被统计并跳过结算。",
      },
      {
        name: "ElectricityManager",
        detail: "单例管理器在服务端主线程按电网周期调度结算，捕获周期异常并记录日志，同时暴露最近一次 PowerCycleStats。",
      },
      {
        name: "PowerCycleStats",
        detail: "记录周期号、拓扑版本、网络与端点数量、线缆数量、总输入、实际交付、损耗、未满足需求和耗时。",
      },
    ],
    settlement: [
      "周期开始前，端点依稳定 BlockKey 顺序执行 beforePowerCycle。",
      "网络先把生产者与可放电储能按公平游标轮转顺序供给消费者。",
      "未用于放电的储能再由生产者充电，避免同一个储能端点在同周期自相矛盾地充放电。",
      "每条路径同时受源端预算、目标需求、线缆吞吐和路径损耗约束；任何输入输出变化都会触发端点的 onPowerChanged。",
    ],
    playerGuidance: [
      "先放置火力发电、储能与基础线缆，再接入需要能量的设备；不要把大规模设备直接堆在未验证的单段网络上。",
      "设备不工作时先查供给、线缆吞吐、储能余量和消费者需求，不要反复拆装导致拓扑持续重建。",
      "出现超大网络统计时，应拆分网络或减少无效连接；被跳过的网络不会得到结算。",
    ],
  },
  coreLoop: {
    title: "游戏核心循环",
    lead: "每一次推进都应留下可见产出，并为下一次选择增加新的处理能力，而不是只把原材料换成更高数字。",
    stages: [
      {
        title: "采集与整理",
        detail: "从原版世界获取基础资源，使用压缩系列、筛网、破碎锤与破碎链把分散材料变成可进入配方的输入。",
      },
      {
        title: "转化与制作",
        detail: "用高级工作台、压缩机、炉釜与烤盘处理不同配方；树脂、火焰材料、超级骨粉与超级线让材料线出现分工。",
      },
      {
        title: "供能与自动化准备",
        detail: "制造电气部件，接通火力发电、储能和线缆。能量预算成为扩大设备规模之前必须解决的约束。",
      },
      {
        title: "探索与能力扩展",
        detail: "防具、法杖、展示器、注能核心、空间尘、末地石尘与引力相关物件把玩家带到科技、魔法、空间和引力的交叉处。",
      },
    ],
    designRule: "循环中的任何跳级都必须付出材料、处理时间或能量中的至少一种成本；没有被当前版本实现的捷径不写进玩家承诺。",
  },
  progression: {
    title: "四阶段进程",
    lead: "这是当前内容的阅读顺序，不是对所有服务器强制执行的等级锁。分类前置关系由向导体系表达，服主可据此安排任务与活动。",
    phases: [
      {
        phase: "第一阶段：立足",
        focus: "基础与材料",
        current: "压缩系列、筛网、破碎锤与破碎链构成资源整理起点。",
        playerGoal: "建立稳定的基础材料周转，不依赖一次性发放。",
      },
      {
        phase: "第二阶段：生长与防护",
        focus: "植物、火焰材料与防御",
        current: "种子、蘑菇、超级骨粉、超级线、火焰材料与防具扩展生存侧选择。",
        playerGoal: "把可再生输入与生存安全变成可管理的资源线。",
      },
      {
        phase: "第三阶段：工业化",
        focus: "科技、机器与电网",
        current: "树脂、电气部件、火力发电、储能、储罐和五类机器建立处理能力与能量约束。",
        playerGoal: "先让电网稳定，再扩大设备规模。",
      },
      {
        phase: "第四阶段：跨域探索",
        focus: "魔法、空间与引力",
        current: "法杖、展示器、注能核心、空间尘、末地石尘与引力相关物件承担高阶探索入口。",
        playerGoal: "以已有工业与材料能力支撑更高风险的探索，而不是绕过前段经济。",
      },
    ],
  },
  planning: {
    title: "27 学科与 810 物品：按纵切交付，不伪装为现状",
    lead: "当前生产 JAR 已注册 150 个稳定物品 ID，并落地 8 个学科根目录。27 学科与 810 条资料库描述完整内容版图；后续以九个可独立验收的纵切波次推进，每次先形成输入、处理、产出、回收和失败边界，再扩下一波。",
    accounting: {
      implementedRoots: 8,
      plannedRoots: 19,
      plannedItemCapacity: 810,
      clarification: "生产实装数以 /runtime 为准；810 是策划容量，不是对发布日期、配方数量或掉落概率的承诺。",
    },
    principles: [
      "先补齐一条可循环的生产链，再新增相邻学科；不以孤立收藏品填充目录。",
      "每个新学科都必须说明前置、产出、消耗、回收和失败成本，避免单向通胀。",
      "跨学科配方只能制造新的决策，不应把所有旧线压缩成唯一最优解。",
      "策划状态与实现状态分栏显示；规划中内容不进入掉落表、配方表、下载说明或玩家保证。",
      "810 容量由完整玩法闭环驱动，不由界面上的空槽或命名数量驱动。",
    ],
  },
  economy: {
    title: "经济、平衡与失败恢复",
    lead: "以下是一期的运营与数值原则。它们不宣称已经存在一套内建货币、市场或远程恢复系统，而是约束当前材料、机器与能量体系如何被服主使用。",
    economy: [
      "资源价值由采集难度、处理时间与能量预算共同决定；任何一种成本都不能被无限免费跳过。",
      "压缩、破碎、筛分、机器处理与电网不是平行的装饰线，而是把低价值输入转换为可选择的下一步。",
      "服主如接入外部经济插件，应把 SoulTech 产物作为服务器政策的一部分重新定价，不应假定本插件已经提供官方货币汇率。",
    ],
    balance: [
      "材料线控制输入稀缺性，机器线控制处理速度，电网控制并发规模；三者应同时调整，不能只提高产出倍率。",
      "线缆吞吐与损耗、储能容量、生产端预算、消费端需求都能通过 PowerCycleStats 观察；先看实际未满足需求与损耗，再做数值调整。",
      "新配方上线前要明确它替代哪一步、保留哪一步和消耗什么，避免一件新物品让整条旧生产线失去意义。",
    ],
    recovery: [
      "玩家侧：机器或电网失效时先停止继续投料，记录位置、物品与最近操作，按供给、连接、保护、配方、持久化顺序排查。",
      "服务器侧：正常 onDisable 会保存机器、方块缓存与在线玩家数据；变更前仍应备份数据库与插件数据目录，不能把正常停服逻辑当作灾备。",
      "云端侧：快照同步用于受控观测与状态记录。未声明的远程回档、跨服物品迁移或自动修复不应被承诺给玩家。",
    ],
  },
  footer: {
    statement: "TalexSoulTech 以实际代码和明确契约定义边界：已实现的能部署，规划中的会被明确标注。",
    privacy: "本站不接入第三方追踪；控制台会话仅用于已登录服主的受控管理。",
  },
};

export const TECH_ARCHITECTURE = {
  title: "一期 Paper 技术架构",
  lead: "下面的拆解仅描述当前 Java 源码已经体现的职责与调用关系。它不把未来机器、未实现学科或云端路线反写成本地插件事实。",
  layers: [
    {
      name: "插件入口与生命周期",
      components: ["TalexSoulTech", "BaseTalex", "MysqlManager", "ElectricityManager"],
      detail: "TalexSoulTech 是 JavaPlugin 入口并保存静态实例。onEnable 保存默认配置、初始化 BaseTalex、注册 Listeners、BlockListener、UIListener 与命令，然后为在线玩家创建 PlayerData。onDisable 先关闭界面和电网，再保存机器、物品、方块和玩家数据，最后关闭 MySQL 连接。",
    },
    {
      name: "玩法事件组合",
      components: ["Listeners", "BlockListener", "MachineManager", "ProtectorManager"],
      detail: "Listeners 负责进入、离开、手持、交互等玩家事件；BlockListener 负责自定义方块放置与破坏；MachineManager 在通过保护检查后用每台机器的 MachineChecker 决定是否打开机器 UI。",
    },
    {
      name: "库存界面",
      components: ["InventoryUI", "UIListener", "MenuBasic"],
      detail: "InventoryUI 管理分页 Inventory2D 与 Holder；UIListener 只接管该 Holder 的点击和关闭事件，并按玩家节流。MenuBasic 将 Setup、按玩家 Setup、打开、重开与销毁包装成通用菜单生命周期。",
    },
    {
      name: "电网结算",
      components: ["PowerGrid", "EnergyBuffer", "PowerCable", "PowerCycleStats", "ElectricityManager"],
      detail: "电网只在服务端主线程结算。它把位置化端点和线缆重建为连通网络，以源、路径和目标的预算进行能量转移，并把周期统计保留给观测与排错。",
    },
    {
      name: "领域与持久化",
      components: ["PlayerData", "TalexItem", "SoulTechItem", "BaseMachine", "CategoryObject", "TalexBlock"],
      detail: "物品、分类、机器、已放置方块和玩家状态各有自己的领域对象；MySQL 与 YAML 缓存承担不同层次的正常停服持久化职责。",
    },
  ],
  power: {
    title: "PowerGrid 的一次周期",
    flow: [
      "ElectricityManager.start 在主线程注册周期任务；runCycleNow 再次校验主线程后调用 PowerGrid.tick。",
      "PowerGrid 复制并排序端点，对每个仍被注册的端点调用 beforePowerCycle；拓扑变更时通过六向邻接重建连通分量。",
      "节点数超过 maxNetworkNodes 的网络被标记为 oversized，并在该周期统计中跳过结算，而不是半算一部分。",
      "正常网络使用公平游标排序生产者、储能与消费者：生产者和储能先向消费者供电，随后未放电的储能只从生产者充电。",
      "每次转移同时检查源 EnergyBuffer 的可提取量、目标可接收量、路线的可用线缆吞吐与 PowerCable 损耗；内部预算不一致会抛出异常而不是静默吞能。",
      "产生变化的端点收到 onPowerChanged，PowerCycleStats 记录总输入、交付、损耗、未满足需求、网络规模和耗时。",
    ],
    invariants: [
      "PowerGrid 在同一 BlockKey 上将端点与线缆互斥注册；新注册项会让 topologyDirty 变为 true。",
      "EnergyBuffer 不允许负请求，receive 与 extract 都返回实际接受或提取的数量，并可在不改变状态时模拟。",
      "PowerCable 拒绝非正吞吐与非法千分比损耗，损耗范围为 0 至 999。",
      "ElectricityManager.runCycleSafely 会记录运行时异常，避免一个周期异常直接停止后续调度。",
    ],
  },
  ui: {
    title: "InventoryUI、UIListener 与 MenuBasic",
    flow: [
      "InventoryUI 用 InventoryUIHolder 标识自己创建的库存，并可建立多页 Inventory2D；翻页按钮由可点击物品实现。",
      "UIListener.onClick 先确认点击的是 InventoryUIHolder，然后把事件交给 UI，再执行当前槽位 ClickableItem。",
      "同一玩家的连续点击受 UI interval 节流；点击项声明处理成功，或 UI 不允许放入物品时，事件会被取消。",
      "关闭时，UIListener 根据 canClose、closed 状态调用 onTryInventoryClose 或仅一次 onInventoryClose。",
      "UIListener 的异步定时器把实际 refresh 切回 Bukkit 主线程，并只刷新当前打开且开启 autoRefresh 的 InventoryUI。",
      "MenuBasic 的 openForPlayer 先运行 SetupForPlayer，再执行只打开动作；destroy 会释放自身保有的 InventoryUI 与菜单引用。",
    ],
    boundary: "界面点击节流和事件取消是 UI 行为控制，不是权限系统；机器操作前的保护检查仍在玩法事件路径中完成。",
  },
  events: {
    title: "Listeners、BlockListener 与 MachineManager 的组合",
    composition: [
      {
        source: "玩家进入与离开",
        detail: "Listeners.onJoin 异步构造 PlayerData；onLeave 查找该玩家数据并调用 leave，触发状态写回。",
      },
      {
        source: "玩家交互",
        detail: "Listeners.onInteract 取 PlayerData，先调用 ProtectorManager，再交给 MachineManager。随后才处理 MachineItem 放置逻辑、guide 标签和 st_items NBT 标签分发。",
      },
      {
        source: "手持物品",
        detail: "Listeners.onItemHold 先验证 TalexItem，再用 SoulTechItem 的物品验证将事件交给匹配的扩展物品。",
      },
      {
        source: "放置方块",
        detail: "BlockListener.onBlockPlaced 过滤原版不适合作为自定义方块的材料，验证 TalexItem 与 soul_tech_item_id；未被物品自行处理时创建 TalexBlock。",
      },
      {
        source: "破坏方块",
        detail: "BlockListener.onBlockBreak 先走保护检查，再让自定义工具处理；若目标是 BlockManager 中的 TalexBlock，则交给其受控破坏逻辑。",
      },
      {
        source: "机器选择",
        detail: "MachineManager 保存以机器名称为键的 BaseMachine。onEvent 依次调用 MachineChecker，首个通过者关闭当前界面并打开机器，随后立即返回。",
      },
    ],
  },
  lifecycle: {
    title: "TalexSoulTech、BaseTalex 与 MysqlManager 的生命周期",
    startup: [
      "TalexSoulTech.onEnable 设置插件实例与前缀，调用 BaseTalex.init，再调用 BaseTalex.enable。",
      "BaseTalex.enable 在 Settings.mysql.enabled 为 true 时连接 MySQL，并建立 soul_tech_player_data 和 soul_tech_system；连接失败会抛出异常阻止继续启动。",
      "随后创建并启用 CategoryManager，创建 MachineManager、BlockManager、ProtectorManager，实例化五类机器。",
      "initBase 载入方块缓存、可恢复的 MachineBlockItem 缓存，启动 ElectricityManager，并读取机器缓存。",
      "MysqlManager.get 是进程内惰性单例入口；ElectricityManager.INSTANCE 是固定单例；BaseTalex 则由静态 init 创建并作为玩法服务枢纽暴露管理器。",
    ],
    shutdown: [
      "TalexSoulTech.onDisable 先关闭在线玩家库存和电网周期，清理全息文本，再保存机器与物品缓存。",
      "BlockManager 正常写入方块缓存；所有 PlayerData 执行 leave 后，MysqlManager.shutdown 关闭 JDBC 连接。",
      "这条顺序服务于正常停服，不保证对进程被强杀或底层存储损坏的自动恢复。",
    ],
  },
  domain: {
    title: "领域模型关系",
    relationships: [
      {
        model: "PlayerData",
        relationship: "绑定 BaseTalex、Bukkit Player、名称、UUID、JSON 状态与 PlayerAttractData；构造时进入 playerManager 并从 soul_tech_player_data 读取，leave 时插入或更新该表。",
      },
      {
        model: "TalexItem → SoulTechItem",
        relationship: "TalexItem 以 ItemStack 和 ItemBuilder 为基础，负责类型、NBT 标签与物品一致性验证。SoulTechItem 继承它，写入 st_items 类型和 soul_tech_item_id，并维护静态物品注册表。",
      },
      {
        model: "BaseMachine → MachineManager",
        relationship: "BaseMachine 维护名称、展示物品、MachineChecker 与配方集合，并在构造时向 MachineManager 自动注册。",
      },
      {
        model: "CategoryObject → RecipeObject → TalexItem",
        relationship: "CategoryObject 是向导树节点，拥有父子关系、前置关系、优先级和菜单或对象类型。对象类型会把 RecipeObject 的展示物品反向关联到自身。",
      },
      {
        model: "TalexBlock → BlockManager → SoulTechItem",
        relationship: "TalexBlock 以位置和原始 ItemStack 注册到 BlockManager，可关联 SoulTechItem。受控破坏会先取消原版事件、执行物品钩子、注销自身、清空方块并掉落保存的物品。",
      },
    ],
  },
};

export const SAAS_ARCHITECTURE = {
  title: "一期 Cloudflare SaaS 架构",
  lead: "一期云端负责服主身份、服务器归属、一次性配对、顺序化快照同步，以及受能力权限约束的 Cordis 风格扩展运行时。Paper 插件仍是游戏规则与本地数据的执行者；云端不接管玩家背包、机器判定或实时电网结算。",
  status: {
    current: "一期已实现",
    scope: [
      "静态站点与 Worker API 同属 site 目录；公开站点只提供内容与控制台入口。",
      "认证、服务器管理、配对与同步全部使用 JSON；错误统一为 {error:{code,message}}。",
      "D1 保存身份、会话、服务器、密钥哈希、配对码、快照与事件；每台服务器由一个 Durable Object 串行处理同步序列。",
      "Cordis 风格扩展以 Context、依赖拓扑、LIFO disposer、原子热更新与 last-known-good 为运行时骨架，Lua 与 JavaScript 都只能通过被授予的能力访问云端资源。",
    ],
    nonClaims: [
      "当前实现不把云端写入当作游戏内状态的唯一真相。",
      "当前实现不承诺跨服物品转移、远程玩家操作、自动回档、远程命令执行或未列出的公开 API。",
    ],
  },
  boundaries: [
    {
      boundary: "浏览器与会话",
      detail: "服主使用用户名和密码注册或登录。密码使用 PBKDF2；会话放在 HttpOnly、Secure、SameSite=Lax Cookie 中，浏览器脚本不读取会话令牌。",
    },
    {
      boundary: "D1 租户数据",
      detail: "DB 绑定指向 D1，其中 users、sessions、servers、server_api_keys、pairing_codes、server_snapshots、server_events 是一期的基础持久化表；扩展控制记录与审计同样按租户和服务器范围持久化。外键与索引服务于用户归属、服务器查询和按服务器写入。",
    },
    {
      boundary: "服务器 API Key",
      detail: "明文 API Key 只在配对成功响应中返回给插件一次；D1 仅保存 SHA-256 哈希。控制台、日志、状态接口和插件命令不回显它。",
    },
    {
      boundary: "Durable Object",
      detail: "SYNC_COORDINATOR Durable Object 绑定为每个 serverId 提供串行化入口。同步的 sequence 判定、快照写入和事件记录在同一服务器顺序内完成，避免同服并发请求抢写最后状态。",
    },
    {
      boundary: "扩展 Context 与沙箱",
      detail: "每个扩展实例获得独立 Context、自己的依赖视图、已授权能力与 disposer 栈。Lua 与 JavaScript 都运行在受限沙箱中，不继承 Worker 全局绑定、其他扩展状态或跨服务器租户的数据访问权。",
    },
    {
      boundary: "Paper 插件",
      detail: "插件配置 cloud.enabled 默认 false。完成 /tst cloud link 配对码 后才保存 serverId、apiBase 和 API Key，并以 Bearer 认证提交状态快照。",
    },
  ],
  api: [
    {
      group: "服务健康",
      routes: [
        { method: "GET", path: "/api/health", detail: "返回 Worker 健康状态；它不泄露会话、服务器密钥或租户快照。" },
      ],
    },
    {
      group: "认证",
      routes: [
        { method: "POST", path: "/api/auth/register", detail: "创建用户名与 PBKDF2 密码凭据，并建立受保护会话。" },
        { method: "POST", path: "/api/auth/login", detail: "校验用户名和密码后建立受保护会话。" },
        { method: "POST", path: "/api/auth/logout", detail: "撤销当前会话并清除会话 Cookie。" },
        { method: "GET", path: "/api/auth/me", detail: "返回当前已登录服主的身份信息。" },
      ],
    },
    {
      group: "服务器管理",
      routes: [
        { method: "GET / POST", path: "/api/servers", detail: "读取当前服主的服务器列表，或在当前服主名下创建服务器。" },
        { method: "GET", path: "/api/servers/:id", detail: "在 current user_id 与 owner 边界通过后读取单台服务器。" },
        { method: "POST", path: "/api/servers/:id/pairing", detail: "为当前服主拥有的服务器生成一次性、十分钟有效的配对码。" },
        { method: "GET", path: "/api/servers/:id/snapshot", detail: "在所有权校验通过后读取该服务器的最近快照。" },
      ],
    },
    {
      group: "插件配对",
      routes: [
        { method: "POST", path: "/api/pair/claim", detail: "未登录插件提交 {code,name,softwareVersion}；有效且未使用的码成功后返回 {serverId,apiKey,apiBase}。" },
      ],
    },
    {
      group: "状态同步",
      routes: [
        { method: "POST", path: "/api/sync", detail: "插件用 Authorization: Bearer API Key 提交 {serverId,sequence,sentAt,server,players,systems,catalog}；成功响应为 {accepted,sequence,serverTime}。" },
      ],
    },
  ],
  tenantIsolation: [
    "管理端所有 servers、pairing 与 snapshot 读写都从当前会话得出 user_id，并验证 servers.owner_id；仅靠前端隐藏服务器不构成授权。",
    "配对码属于一台已归属服务器，只能成功领取一次，且十分钟后失效。领取接口不要求浏览器会话，但必须把产生的 serverId、名称与版本绑定到有效码。",
    "同步 API 先通过 Bearer API Key 的哈希校验定位服务器；提交的 serverId 必须与该凭据对应的服务器一致，不能借其他服务器 ID 写入。",
    "每一台服务器的 sequence 在自己的 Durable Object 内串行处理，服务器 A 的写入不会阻塞或污染服务器 B。",
    "扩展控制面同样以 current user_id、owner_id 与 serverId 为边界；扩展 Context 与能力句柄只在该服务器租户中有效，审计事件也不能跨租户读取。",
  ],
  pairing: {
    title: "一次性配对契约",
    steps: [
      "已登录服主在自己拥有的服务器详情页请求 POST /api/servers/:id/pairing。",
      "控制台展示一枚单次、十分钟有效的配对码；它不是长期 API Key。",
      "插件以未登录请求 POST /api/pair/claim，并提交配对码、服务器名称与软件版本。",
      "成功响应把 serverId、apiKey、apiBase 交给插件。插件本地保存这些值，但 status 与 link 命令不打印 apiKey。",
      "此后插件只用 Bearer API Key 访问 POST /api/sync；服主仍通过浏览器会话管理服务器与查看快照。",
    ],
  },
  sync: {
    title: "快照同步契约",
    guarantees: [
      "请求体把服务器元信息、在线玩家概览、系统状态和目录摘要打包为一条服务器快照；它不发送玩家私有背包或数据库凭据。",
      "Durable Object 以 serverId 分片，顺序化同一服务器的 sequence 决策与持久化，从而避免并发同步覆盖。",
      "API 返回 accepted、最终 sequence 与 serverTime，插件据此判断本次状态是否被接受并调整下一次同步。",
      "同步失败应保持本地游戏继续运行，待下一周期重试；不要因为云端短暂不可用阻塞 Paper 主线程。",
    ],
  },
  extensions: {
    title: "Cordis 风格云端扩展运行时",
    status: "一期已实现",
    lead: "扩展不是能随意读取 Worker 绑定的脚本片段，而是由云端控制面创建、在服务器租户 Context 中运行、受依赖图与能力权限约束的生命周期单元。",
    context: {
      title: "Extension Context",
      detail: "每个已启动扩展拥有独立 Context：其中保存扩展标识、所属服务器与租户、解析后的依赖、已获授权的能力、运行状态和专属 disposer 栈。扩展之间不共享可变全局状态，Context 是资源与授权的唯一宿主。",
      rules: [
        "Context 由控制面创建并绑定到一台已归属服务器；扩展不能通过参数伪造其他 serverId 或 owner_id。",
        "任何通过 Context 取得的资源都在取得时登记 disposer，避免启动失败、停用或更新后遗留定时器、句柄、订阅或沙箱状态。",
        "Context 销毁后，能力句柄立即失效；已停止扩展不能继续读取快照、写入状态或追加审计。",
      ],
    },
    dependencyTopology: {
      title: "依赖拓扑",
      detail: "控制面将扩展清单中的依赖构成有向图，在启动或更新前检查缺失依赖、循环依赖和版本约束。只有依赖全部处于可用状态时，目标扩展才可启动。",
      lifecycleOrder: [
        "启动按拓扑顺序进行：先依赖，后依赖者。",
        "停止与删除按反向拓扑进行：先依赖者，后其依赖，避免上游资源先被释放。",
        "任一节点启动失败时，当前候选链路回收已经取得的资源，不把半初始化的扩展标成运行中。",
      ],
    },
    disposal: {
      title: "LIFO disposer",
      detail: "Context 使用后进先出释放策略。资源通常有依赖建立顺序：最后登记的订阅、任务或桥接句柄最先释放，先登记的底层资源最后关闭。",
      guarantees: [
        "停用、停止、删除、热更新失败和启动失败都走同一条 disposer 路径。",
        "单个 disposer 异常会被记录为审计与运行错误，但不会中断后续 disposer 的释放。",
        "释放过程完成前，扩展不会被标记为已彻底停止；这样控制台状态与实际资源状态保持一致。",
      ],
    },
    hotUpdate: {
      title: "原子热更新与 last-known-good",
      detail: "更新从候选包与候选配置开始，而不是原地改写正在运行的实例。控制面先校验清单、依赖图、授权能力和沙箱装载，再在独立候选 Context 中启动。",
      flow: [
        "验证候选扩展的标识、版本、依赖、配置结构与能力声明。",
        "在不影响当前运行实例的候选 Context 中装载 Lua 或 JavaScript 沙箱，并等待启动完成。",
        "候选启动成功后，原子切换活动版本指针；新的 Context 成为唯一可接收控制面操作的实例。",
        "旧实例按 LIFO disposer 完整退出；成功运行的版本与已解析配置被保存为 last-known-good。",
        "候选任一步失败时，候选 Context 立即释放，活动实例和 last-known-good 保持不变，不以部分更新覆盖线上扩展。",
      ],
      operatorMeaning: "热更新成功才改变活动版本；失败是可审计事件，不是要求服主手工猜测旧包内容的状态。",
    },
    sandboxes: [
      {
        runtime: "JavaScript",
        detail: "每个 JavaScript 扩展在独立受限执行域中运行，只通过宿主桥接得到 Context 和已授权能力；它不能直接枚举 Worker 环境变量、D1 绑定、Durable Object 绑定或其他扩展的内存。",
      },
      {
        runtime: "Lua",
        detail: "每个 Lua 扩展运行在独立受限状态中，宿主只注入与 Context 能力对应的接口；无权访问的主机函数、跨租户状态和未声明的网络或持久化资源不可见。",
      },
    ],
    capabilities: {
      title: "权限能力，而不是全局权限",
      detail: "扩展清单声明所需能力，控制面在安装、更新与启动前核对授权。运行时把被批准的能力作为窄句柄注入 Context；没有得到句柄的操作默认拒绝。",
      rules: [
        "能力按服务器租户作用域裁剪，不能用一个扩展的授权操作另一台服务器。",
        "能力可覆盖受控状态读取、受控状态写入、允许的外部调用和审计追加，但每类动作都必须由清单与控制面共同允许。",
        "能力变更属于控制面变更：它会进入审计，并在下一次原子启动或热更新中生效，而不是在运行中悄悄放大权限。",
      ],
    },
    controlPlane: {
      title: "云端增删、启停与审计",
      detail: "已登录服主只能在自己拥有的服务器范围内创建、编辑、安装、更新、启用、禁用、启动、停止和删除扩展。控制面负责状态机与资源清理，不把这些操作下放给浏览器本地状态。",
      actions: [
        { action: "创建与编辑", outcome: "保存扩展标识、清单、配置和依赖声明，但不会绕开权限与依赖校验直接运行。" },
        { action: "启动与启用", outcome: "验证依赖图与能力后创建 Context；成功才标记为运行中。" },
        { action: "停止与禁用", outcome: "阻止新工作进入，按依赖反序和 LIFO disposer 释放资源，再更新状态。" },
        { action: "热更新", outcome: "在候选 Context 中验证并启动，原子切换或保留 last-known-good。" },
        { action: "删除", outcome: "先完成停止与资源释放，再删除该服务器范围内的扩展记录；不会跨租户删除同名扩展。" },
      ],
    },
    audit: {
      title: "可追溯审计",
      detail: "每次控制面动作都会记录发起用户、所属租户与服务器、扩展标识、动作、结果、时间、关联版本和失败原因摘要。敏感配置、API Key 和沙箱源代码不写入可见审计字段。",
      events: [
        "创建、编辑、授权变更、安装、启用、禁用、启动、停止、删除。",
        "依赖图拒绝、权限拒绝、沙箱装载失败、disposer 失败、热更新成功与回退到 last-known-good。",
      ],
    },
  },
  roadmap: {
    title: "后续路线，尚不属于当前接口承诺",
    items: [
      { area: "观测", state: "后续路线", detail: "在现有快照与事件基础上增加趋势图、异常阈值和可下载的运营报表。" },
      { area: "运维", state: "后续路线", detail: "研究密钥轮换、快照保留策略和细粒度服主成员权限，但不改变当前 owner 边界。" },
      { area: "内容运营", state: "后续路线", detail: "让目录与版本元数据获得审核发布流程，但不允许云端未经审计地下发游戏规则。" },
    ],
  },
};

export const TUTORIALS = [
  {
    id: "quick-install",
    label: "快速安装",
    title: "从受控 JAR 到第一位玩家",
    summary: "以 Java 25、Paper 26.1.2.build.74-stable 和已连接 MySQL 为前提，完成最小风险的本地上线。",
    steps: [
      { title: "核对运行时", detail: "确认服务器运行 Java 25，并使用 Paper 26.1.2.build.74-stable。" },
      { title: "投放构建产物", detail: "使用与当前提交对应的 talex-soul-tech-3.0.0-SNAPSHOT.jar，放入 plugins 目录并保留旧版本备份。" },
      { title: "配置持久化", detail: "为 Settings.mysql 填入受控私网中的数据库连接信息，将 Settings.mysql.enabled 设为 true。" },
      { title: "完成首次启动", detail: "等待插件创建所需表、初始化分类、机器、方块缓存与电网，再开放玩家入口。" },
      { title: "走一遍真实玩家路径", detail: "用新玩家进入服务器，确认收到向导书；使用 /tst 查看插件帮助。" },
    ],
    notes: [
      "没有确认发行来源时，不要给玩家提供下载链接。",
      "配置 cloud.enabled 之前无需也不应填入任何云端 API Key。",
    ],
  },
  {
    id: "mysql-persistence",
    label: "MySQL",
    title: "让玩家状态可恢复地落库",
    summary: "PlayerData 在进入时读取数据、离开时写回。生产服应把数据库作为上线前置，而不是故障后的补救。",
    steps: [
      { title: "建立隔离数据库", detail: "为 TalexSoulTech 使用独立数据库与专用账户，不与不相关插件共用高权限账号。" },
      { title: "按实际网络授权", detail: "只允许 Minecraft 主机或容器的真实来源网段访问，使用 SELECT、INSERT、UPDATE、DELETE、CREATE、ALTER、INDEX 的最小权限集合。" },
      { title: "填写插件配置", detail: "设置 Settings.mysql.ip、port、db、user、pass 与 enabled；凭据仅保留在服务器受保护配置中。" },
      { title: "确认建表路径", detail: "连接成功后插件会建立 soul_tech_player_data 与 soul_tech_system；新玩家数据将进入前者。" },
      { title: "安排备份", detail: "在版本变更和停服前备份数据库与插件数据目录；正常 onDisable 保存不是灾备替代。" },
    ],
    diagnosis: [
      { symptom: "连接失败", action: "先检查数据库可达性、实际容器来源网段、数据库名和专用账户权限，再允许玩家登录。" },
      { symptom: "Public Key Retrieval is not allowed", action: "仅在受控私网核对 MySQL 8 认证与 JDBC 连接参数；不要借此把数据库暴露到公网。" },
      { symptom: "玩家数据无法保持", action: "检查 mysql.enabled、连接是否成功、soul_tech_player_data 是否存在，以及玩家离开时是否触发正常保存。" },
    ],
  },
  {
    id: "guidebook",
    label: "向导书",
    title: "从向导书进入分类与配方",
    summary: "向导书是进程入口，不是装饰品。它根据玩家是否已安装、是否有上次阅读位置决定打开首次引导、历史位置或完整目录。",
    steps: [
      { title: "确认首次发放", detail: "新玩家进入时若没有向导书，系统会发放带 guide 类型的灵魂科技向导书。" },
      { title: "从基础开始", detail: "先阅读基础、材料、植物与防御，再按分类前置进入科技、魔法、空间与引力。" },
      { title: "保留阅读上下文", detail: "重新使用向导书时，系统优先恢复 lastGuider；没有记录时进入完整向导目录。" },
      { title: "识别规划边界", detail: "目录中的规划中学科只说明方向，不代表玩家能在当前服务器获得物品、配方或解锁。" },
    ],
    notes: ["不要用改名的原版书替代系统向导书；交互路由依赖物品类型标记。"],
  },
  {
    id: "machine-flow",
    label: "机器",
    title: "用正确的事件路径打开五类机器",
    summary: "机器由 BaseMachine 注册、MachineChecker 识别、MachineManager 打开，保护检查先于机器界面。",
    steps: [
      { title: "按当前分类准备输入", detail: "高级工作台、破碎锤机器、压缩机、炉釜机器和烤盘机器是当前已注册的五类机器。" },
      { title: "在允许交互的位置操作", detail: "玩家交互先经过 ProtectorManager；保护失败不应尝试通过命令或物品标签绕开。" },
      { title: "让检查器选择机器", detail: "MachineManager 依次检查已注册机器；首个匹配 MachineChecker 的机器会打开自身 UI。" },
      { title: "正常停服后复查", detail: "机器数据在正常禁用时写入缓存，下次启动由机器管理器读取；变更前仍需备份。" },
    ],
    diagnosis: [
      { symptom: "右键没有打开机器", action: "先确认位置未被保护拦截、玩家 PlayerData 已载入、目标对应当前五类机器之一。" },
      { symptom: "打开了错误界面", action: "检查多个 MachineChecker 的匹配条件，机器管理器会采用首个匹配项。" },
    ],
  },
  {
    id: "power-grid",
    label: "电网",
    title: "从火力发电到可观测供能",
    summary: "用火力发电、储能、线缆和消费端组成小网络，先看供给与吞吐，再增加设备。",
    steps: [
      { title: "先建立供给", detail: "从当前已确认的火力发电、储能和电气部件开始，不要先堆叠消费者。" },
      { title: "明确连接范围", detail: "端点与线缆按六向邻接组成网络；无效连接会扩大拓扑，超大网络会被跳过结算。" },
      { title: "检查预算", detail: "确认源端存量与每周期输出、线缆吞吐和损耗、目标需求与每周期接收上限同时成立。" },
      { title: "观察周期统计", detail: "使用 PowerCycleStats 中的交付、损耗、未满足需求和耗时判断瓶颈，而不是只看某一台设备。" },
      { title: "分段扩容", detail: "负载持续增加时分割网络或增加合适的供给与储能，避免触发超大网络保护。" },
    ],
    diagnosis: [
      { symptom: "消费者没有能量", action: "依次检查电源缓存、源端每周期预算、线缆吞吐与路径、目标接收上限和未满足需求。" },
      { symptom: "网络不结算", action: "检查是否超过节点上限导致 oversized 网络被跳过，必要时拆分拓扑。" },
      { symptom: "供能不稳定", action: "不要让储能在同周期既被当作消费者又被当作供给者；当前结算会优先放电后只给未放电储能充电。" },
    ],
  },
  {
    id: "cloud-pairing",
    label: "云端配对",
    title: "把一台 Paper 服务器安全接入控制台",
    summary: "配对码是十分钟内可使用一次的临时凭据；长期同步使用只保存在插件本地的 API Key 哈希验证链路。",
    steps: [
      { title: "创建服务器记录", detail: "服主登录控制台后，在自己的服务器列表中创建或选择服务器。" },
      { title: "生成配对码", detail: "在拥有该服务器的会话中请求配对；每个码只能领取一次，并在十分钟后失效。" },
      { title: "由插件领取", detail: "由拥有 talex.soultech.admin 权限的管理员在目标服务器执行 /tst cloud link 配对码。插件提交配对码、服务器名称和软件版本。" },
      { title: "保留密钥边界", detail: "插件获得 serverId、apiBase 和 API Key。API Key 只写入本地受保护配置；/tst cloud status 只报告链接与同步状态，link 与 status 都不回显密钥。" },
      { title: "开始周期同步", detail: "插件使用 Bearer API Key 向 /api/sync 提交 serverId、sequence、sentAt、server、players、systems、catalog。" },
    ],
    notes: [
      "cloud.enabled 默认 false；没有完成配对前不应主动请求云端。",
      "控制台按 owner_id 校验服务器归属，配对不能把已归属服务器交给其他账号。",
    ],
  },
  {
    id: "cloud-extensions",
    label: "云端扩展",
    title: "以 Context 和 last-known-good 管理云端扩展",
    summary: "扩展的操作顺序是：声明依赖与能力，经过租户授权，在独立 Lua 或 JavaScript 沙箱启动；更新始终从候选 Context 开始，失败时保留已知良好版本。",
    steps: [
      { title: "在正确服务器范围创建扩展", detail: "服主先进入自己拥有的服务器。扩展记录、配置、Context、能力与审计都绑定该 serverId，不能被另一个租户复用。" },
      { title: "声明而非猜测依赖", detail: "把所需扩展、版本约束和能力写入清单。控制面会先检查缺失项、循环和授权，再允许启动。" },
      { title: "选择受控运行时", detail: "Lua 与 JavaScript 均在各自沙箱中运行，只可通过 Context 获得被批准的宿主能力；不要把数据库、Worker 绑定或 API Key 写入扩展代码。" },
      { title: "启动并观察状态", detail: "依赖按拓扑顺序启动。启动成功才会进入运行中；失败实例会按 LIFO disposer 回收，不留下半开的句柄。" },
      { title: "使用原子热更新", detail: "提交候选版本后，控制面在独立候选 Context 中校验并启动。成功才切换活动版本，失败会保留运行实例与 last-known-good。" },
      { title: "有序停止或删除", detail: "先停止依赖者，再停止其依赖。删除同样先释放资源；不要用删除记录代替停止，因为记录消失不等于运行资源已释放。" },
    ],
    diagnosis: [
      { symptom: "扩展无法启动", action: "先查看审计中的依赖图、版本约束和能力拒绝项。不要通过扩大全局权限或关闭沙箱来绕过失败。" },
      { symptom: "热更新失败", action: "确认候选清单、配置、依赖与能力；当前活动实例和 last-known-good 会保留，可修正候选后再次提交。" },
      { symptom: "停止后仍有影响", action: "查看 LIFO disposer 的审计结果；释放失败会被记录，控制面不会把它伪装成干净停止。" },
      { symptom: "看到其他服务器的数据", action: "这违反 Context 的 serverId 与 owner 边界，应立即停止扩展并检查能力授予、审计关联和控制面授权路径。" },
    ],
    notes: [
      "扩展能力是最小可用句柄，不是 JavaScript 或 Lua 获得 Worker 全局权限的通行证。",
      "审计记录动作与结果，但不会把 API Key、敏感配置或沙箱源码写入可见字段。",
    ],
  },
  {
    id: "troubleshooting",
    label: "排错",
    title: "按边界排错，而不是反复重装",
    summary: "从运行基线、持久化、玩家事件、电网和云端契约依次缩小问题范围；每一步都保留可回滚的证据。",
    steps: [
      { title: "插件无法启用", detail: "核对 Java 25、Paper 26.1.2.build.74-stable、JAR 来源和 MySQL 连接。连接失败会阻止 BaseTalex.enable 继续完成。" },
      { title: "玩家没有向导书或机器无响应", detail: "确认 PlayerData 是否载入、玩家是否通过保护检查，以及交互物品或方块是否经过系统事件入口。" },
      { title: "机器状态丢失", detail: "区分正常禁用时的缓存保存与异常中断；先恢复数据库和 plugins/TalexSoulTech 备份，再检查机器缓存是否匹配当前版本。" },
      { title: "电网异常", detail: "读取最近周期的网络数、未满足需求、损耗与超大网络情况，先拆分或限流，再调整产能。" },
      { title: "控制台看不到服务器", detail: "检查当前浏览器会话的 owner 边界；服务器列表、详情、配对和快照都不会跨 user_id 返回数据。" },
      { title: "配对或同步失败", detail: "检查配对码是否已用或过期、serverId 是否与 API Key 一致、Bearer 头是否存在以及 sequence 是否按服务器顺序递增。" },
      { title: "扩展无法启动或更新", detail: "按审计记录检查 Context 的服务器归属、依赖拓扑、能力授权、Lua/JavaScript 沙箱装载和 LIFO disposer；失败候选应保留 last-known-good，而不是用全局权限强行启动。" },
    ],
    diagnosis: [
      { symptom: "API 返回错误 JSON", action: "读取 error.code 和 error.message；不要靠猜测路径或修改浏览器本地状态绕过服务端授权。" },
      { symptom: "同步没有新快照", action: "确认 API Key 只对应当前 serverId，检查 sequence、sentAt 与 Durable Object 的单服顺序，不在多台服之间复用密钥。" },
      { symptom: "想恢复旧状态", action: "先使用数据库与插件目录备份。当前云端快照不承诺替代完整恢复或跨服迁移。" },
    ],
  },
];
