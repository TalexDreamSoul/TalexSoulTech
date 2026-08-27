// Static campaign planning data. Runtime registration remains independent and is defined by runtime-catalog.js.
const RAW_CAMPAIGN = {
  "campaign": {
    "premise": "旧文明先失去资源来源与消耗账，继而用无界自动化放大欠账，最后借灵魂与空间接口把能量、货物和责任推向无人能解释的远端；当停机、返航和交接记录同时断裂，城邦才崩解。灵魂媒介能够承载能量状态与身份绑定，但本身不无源产能。遗迹只留下可测量的材料链、故障记录、样本、坐标和公共设施残片。复原者从低风险的整理与回生开始，逐波恢复能力，并为每次扩张保留来源、有限能源、容量、停机、恢复和责任归属，最终建立不依赖单一英雄、也不淘汰早期系统的长期城邦。",
    "playerIdentity": "玩家是“复原者”：任何玩家都可承担并交接的守恒工程师、现场调查员与公共维护者。其资格来自测量、建造、修复、记录和负责任的选择，不来自血统、神谕或单人救世许可；个人可以启动一段复原，多人协作和可审计制度才能让它持续。",
    "centralQuestion": "我们能否在恢复更强的工业、灵魂与跨域能力时，始终说清每份输入从哪来、能量由谁支付、系统何时停、失败怎样复原，以及责任如何交给下一位玩家？",
    "ending": "城邦的完成不是一件终极物品，而是故障后仍能停机、对账、恢复和交接：前八波继续提供材料、能源、样本、维护与记录，复原者可以离线，公共服务仍能沿最后有效状态运行。"
  },
  "acts": [
    {
      "id": "A1",
      "order": 1,
      "title": "第一幕·立足与回生",
      "waveIds": [
        "W1",
        "W2"
      ],
      "choice": "在“尽快扩产”和“保留库存缓冲、来源记录与回退余量”之间取舍；允许玩家选择工艺顺序，但任何路线都不能把压缩、取水或生长误写成无源增产。",
      "need": "W1 先用 `basic.wood-compression.plank-9`、筛分与有限供能建立可重复工坊；W2 再把有限水、土壤、种苗和副产物接成生计循环。W1 解决“怎样可靠加工”，W2 解决“怎样不靠持续掠夺维持据点”，而 W2 形成的连续负载自然提出 W3 的安全需求。",
      "revelation": "旧文明的第一处裂缝不是缺少高级科技，而是把资源的表示形式当成产量、把来源与消耗从账上抹掉。压缩本身不是罪因；失去来源、可逆性和库存核对才是。"
    },
    {
      "id": "A2",
      "order": 2,
      "title": "第二幕·边界与维护",
      "waveIds": [
        "W3",
        "W4",
        "W5"
      ],
      "choice": "在吞吐与冗余、自动运行与人工复位、私人订单与公共储备、立即开采与先行勘探之间作出有代价的选择；把停机和维护当作能力，而不是剧情惩罚。",
      "need": "W3 为人员、结构和电网建立撤离、隔离与复工边界；节点增多后，W4 用物流、自动控制和合约处理跨站流动与责任；订单随后暴露深层材料缺口，W5 才以勘探、分选、冶炼和维护提供合格构件。每一波都消费前一波的可读状态，而非重新发明输入。",
      "revelation": "旧文明的第二处裂缝是把“永不停机”当成效率：自动化只是更快复制缺料、满载、错配和过载；材料捷径则把未验证的风险借给未来。"
    },
    {
      "id": "A3",
      "order": 3,
      "title": "第三幕·试验与回程",
      "waveIds": [
        "W6",
        "W7"
      ],
      "choice": "只有测量、容器、影响处理、责任人、载荷、能源预算与返航条件齐全才扩大远征和跨域能力；证据不足时选择暂停、补测或减载，而不是让魔法替玩家跳过前置。",
      "need": "W5 留下未知粉尘、过程液体和远端材料需求；W6 先建立本地基线，再完成有限远征、返程分析、化学处理与环境复测。W7 消费这些带来源的样本和路线记录，为已知端点之间的修复运输补上有主意图、有限载荷、能源预留与恢复归属。",
      "revelation": "灵魂媒介可以承载能量状态并绑定身份、用途和撤销权，但不会无源产能；空间和引力也不会消除距离、重量与返航成本。高阶能力的第一项成果，是能够证明何时不应启动。"
    },
    {
      "id": "A4",
      "order": 4,
      "title": "第四幕·共同体与长时",
      "waveIds": [
        "W8",
        "W9"
      ],
      "choice": "在远界掠取与最小扰动、个人控制与公共授权、当前吞吐与长期储备之间选择；最终把能力交给可撤销角色、公开记录和维护窗口，而不是交给一个永不离线的英雄。",
      "need": "W8 将 W7 的已知端点运输扩展为面对未知航向、生态、污染和相位漂移的完整远征，并把星图、样本与异常带回；W9 再将这些远距档案与前八波生产链编排成可重跑的公共服务周期。",
      "revelation": "旧文明最后失去的不是某台机器，而是可交接的责任：没人能说明谁可启动、谁承担代价、哪份记录有效、故障后由谁接手。终局因此是制度化复原，而不是个人完成一次拯救。"
    }
  ],
  "waves": [
    {
      "id": "W1",
      "order": 1,
      "title": "W1 工业奠基：把第一条守恒线接上电",
      "state": "implemented",
      "disciplineIds": [
        "basic",
        "materials",
        "technology"
      ],
      "purpose": "用 basic、materials、technology 三条相互咬合的证据建立工业底线：原料可整理且可回退，热与韧性有代价，供能能看见、能停机、能恢复。",
      "motivation": "玩家不是为了填满目录，而是要让一处工坊第一次可靠地产出：能把木材压进可运输批次，能从筛分中知道代价，能把耐热/韧性材料送到正确工位，并在供能异常时亲手停机和恢复。完成这条证据链，才值得把生产规模交给后续的农业和水利系统。",
      "crisis": "旧文明把无限抽取、无界自动化和跨域联动当成增长，直到库存无法解释、热工失控、设备在无反馈状态下持续吞料，城邦因此崩解。复原者面对的第一场危机不是缺一件高级装备，而是一次失败作业会让人无法回答“投入去了哪里、谁在耗电、怎样安全停下来”；若不能建立守恒和回退证据，后续每门学科都会把同一错误放大。",
      "continuityIn": "玩家从原版生存资源和一处尚未整理的工位进入本章；810 个策划条目已全部绑定到可操作运行记录，生产目录共 926 个唯一 runtime。W1 门禁只采信可观察的配方、耐久、能量与恢复证据，不以收集数量代替行为验收。",
      "continuityOut": "玩家离开 W1 时带走的不是“集齐了多少物品”，而是一份可复核的原料账、一个能重复作业的筛分/压缩工位和一条能观察供能、主动停机并恢复的最小工作回路。W1 的维护与信号锚点均可实际使用，W2 的 `botany`、`agroecology`、`hydrology` 据此前置票据接入可再生原料与水流。",
      "gate": "门禁不检查玩家是否持有或收集齐 810 项。玩家需完成一段可复核行为证据：1）用 `basic.wood-compression.plank-9` 对 9 块木板执行压缩并潜行丢弃还原，前后清点相等；2）使用筛网完成一轮筛分，记录输入、产出和耐久/停机结果，并验证维护夹只恢复有限耐久；3）用火力发电机、铁质导线、基础蓄电池、保护/信号单元和已注册机器完成一次有燃料运行、断开供给后停机的演示；4）清除故障后从同一 operationId 恢复，证明未提交输入保留且只结算一次。只有同时具备库存前后账、机器运行/停止证据和失败后的恢复路径才通过。",
      "verificationGate": "运行门禁：对任一已注册压缩配方完成一次压缩→还原事务，账本证明输入与返回守恒；再用发电机、导线、储能、信号保护和机器完成一次有源供能作业，记录 milli-SE source debit、delivery、line loss 与机器输入/输出。作业中断开供给，必须观察到停机、未提交输入保留；恢复连接后以同一 operationId 续作一次。门禁不得依赖 catalog 收集数量、voltage 或免费魔能。",
      "disciplineArcs": [
        {
          "id": "basic",
          "role": "生存整理与初级资源守恒：把采集物变成可计数、可运输、可回退的工作库存。",
          "whyNow": "开局背包和工位容量有限，若不先证明压缩的可逆性与筛分的损耗边界，后续材料和供能投入只会把不可解释的浪费放大。",
          "input": "原版木板、木棒、筛分输入和可用工作台；叙事主线绑定 `basic.wood-compression.*` 与 `basic.sieving.*`，不把其余 8 个 family 误写成 W1 必修。",
          "output": "一个可复核的密度阶梯（×9、×81、×729）和一个带输入/产出/耐久记录的筛分工位，为材料与机器提供有来源的基础库存。",
          "recovery": "`basic.wood-compression.plank-9` 允许用潜行丢弃验证还原；高阶压缩只在需求已证实后进行，缺料就回到低阶库存。筛分到耐久边界立即停机，使用 `basic.sieving.mesh-repair-clamp` 执行有限维护；材料或冷却不满足时保留未筛原料并更换备用筛网。"
        },
        {
          "id": "materials",
          "role": "材料属性与代价层：把热耐受、韧性和可回收性变成机器选择的前置条件，而不是装饰标签。",
          "whyNow": "基础资源开始进入火力和设备，材料选错会以高温损坏、筛网停机或共享库存枯竭的形式反复出现。",
          "input": "basic 的压缩库存、筛分产物、木棒、铁材和强力丝线，以及冶炼锅炉路径；`materials.fire-materials.*` 与 `materials.reinforced-thread.*` 的完整三档材料共同约束热边界、韧性与维护成本。",
          "output": "一张按热、韧性和库存机会成本作出的材料选择记录：火焰棒→火焰锭→火焰块对应不同承诺，强力丝线明确哪些库存应留给筛网、护具或维护。",
          "recovery": "工位拒绝或材料不匹配时回到上一档材料和未处理库存，不引入 catalog 未声明的替代配方。强力丝线、线卷与韧性编织网按配方和有限库存参与修复，任何失败都不得无源补料。"
        },
        {
          "id": "technology",
          "role": "可审计供能与安全停机：把材料工位接到一条有来源、有负载、有回退的灵魂电能回路。",
          "whyNow": "机器一旦接电，错误就会从单件材料浪费升级为持续吞料和隐性停机；W1 必须先建立“看得见、关得掉、恢复得了”的边界。",
          "input": "火力发电、铁质导线、基础蓄电池、压缩机与既有机器工位，以及 `technology.machine-network.*` 和 `technology.signal-basics.*` 提供的审计、隔离与停机记录。",
          "output": "一条最小供能闭环：有燃料时负载运行，保护与信号单元能报告状态并隔离故障，断开来源后明确停机，玩家能按步骤恢复且同一输入只结算一次。",
          "recovery": "供能或状态异常时先停止送料，由 machine-network 记录故障，再由 signal-basics 隔离非关键支路；检查燃料、导线和实体连接后按 checkpoint 复位，未提交输入与未用能量预留必须保留或释放。"
        }
      ],
      "familyLinks": [
        {
          "from": "basic.wood-compression",
          "to": "basic.sieving",
          "kind": "supports",
          "reason": "压缩释放背包与工位容量，才能把筛分输入、产出和备用材料同时带进工作区；这是容量关系，不新增未声明配方。"
        },
        {
          "from": "basic.wood-compression",
          "to": "technology.registered-machines-a",
          "kind": "supports",
          "reason": "批量木材为基础机器工位提供可运输的建造库存；先证明压缩/还原行为，再把材料投入机器，而不是按目录收集。"
        },
        {
          "from": "basic.sieving",
          "to": "materials.reinforced-thread",
          "kind": "supports",
          "reason": "高级筛网的连续作业把耐久消耗暴露出来，强力丝线成为应预留的维护库存。"
        },
        {
          "from": "materials.fire-materials",
          "to": "technology.registered-machines-b",
          "kind": "supports",
          "reason": "火焰棒的获得路径经过冶炼锅炉，连接材料属性与实际热工位；关系不等于新增跨学科配方。"
        },
        {
          "from": "materials.fire-materials",
          "to": "technology.thermal-generation",
          "kind": "supports",
          "reason": "火焰锭为高热设备提供材料选择约束，提醒玩家先核对耐热需求再把库存交给供能设施。"
        },
        {
          "from": "materials.reinforced-thread",
          "to": "basic.sieving",
          "kind": "supports",
          "reason": "韧性编织网与筛网修复夹共同构成有限维护链：前者提供可追溯材料，后者恢复有上限的耐久，事故恢复必须实际扣除对应输入。"
        },
        {
          "from": "technology.electric-components",
          "to": "technology.thermal-generation",
          "kind": "supports",
          "reason": "已实装导线是当前可观察供能链的连接边界，火力发电机是供能端；玩家必须用实际连接和断开行为证明状态变化。"
        },
        {
          "from": "technology.signal-basics",
          "to": "technology.electric-components",
          "kind": "supports",
          "reason": "信号线圈建立在铁质导线的连接边界上，负责把过载与停机原因变成可读状态，不能替代实际断路与维修。"
        },
        {
          "from": "technology.signal-basics",
          "to": "technology.energy-storage",
          "kind": "supports",
          "reason": "控制状态与基础蓄电池是两种独立运行证据：一个负责停机语义，一个负责能量缓冲，任何恢复流程都必须分别核对。"
        },
        {
          "from": "technology.machine-network",
          "to": "technology.registered-machines-a",
          "kind": "supports",
          "reason": "机器观测入口以压缩机的运行/停机作为行为样本，记录负载、输入与故障后再允许自动化继续投料。"
        },
        {
          "from": "technology.machine-network",
          "to": "technology.signal-basics",
          "kind": "supports",
          "reason": "机器观测站与信号单元组成“看见异常后安全停机”的已实装链路；重复告警不得重复扣能或重放已提交输入。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "basic",
          "familyId": "wood-compression",
          "familyKey": "basic.wood-compression",
          "reason": "真实 family `wood-compression`（木板压缩）是 W1 的第一条守恒证据：玩家能看见输入、密度和回退边界，而不是只看到更大的物品名称。",
          "stories": [
            {
              "itemId": "basic.wood-compression.plank-9",
              "order": 1,
              "text": "复原者先把 9 块木板压进一格，并用潜行丢弃验证可还原；动机是立刻释放背包与工位容量，失败教训是密度不能被当成凭空增产。"
            },
            {
              "itemId": "basic.wood-compression.plank-81",
              "order": 2,
              "text": "将九个 ×9 压缩板合成为 ×81，玩家先比较压缩前后的库存账，再把它用于搬运；代价是九份材料被锁进一个更难即时拆分的批次，缺货时应退回低阶库存而不是继续压缩。"
            },
            {
              "itemId": "basic.wood-compression.plank-729",
              "order": 3,
              "text": "只有在一次完整工位作业已经证明需要大批量木材时才投入 ×729；它代表把守恒库存交给工业建设的承诺，机器停机或需求改变时不得把高密度方块误当成新增资源。"
            }
          ]
        },
        {
          "disciplineId": "basic",
          "familyId": "sieving",
          "familyKey": "basic.sieving",
          "reason": "真实 family `sieving`（筛分工具）把资源回收与可见磨损绑在一起，给“材料守恒”一个会失败、能恢复的玩家行为，而不是收集更多目录条目。",
          "stories": [
            {
              "itemId": "basic.sieving.normal-mesh",
              "order": 1,
              "text": "安装普通筛网后执行有限次数的基础筛分，并记录每轮输入、产出和剩余耐久；动机是把采集变成可核对的回收流程，代价是筛网寿命，失败时应停机换网而非继续吞入原料。"
            },
            {
              "itemId": "basic.sieving.advanced-mesh",
              "order": 2,
              "text": "用更高的前置材料换取更长的连续作业窗口，并对比单位耐久的产出；若输入、输出或耐久记录不一致，先停止工位、保留样本，再追查配方，不以“筛过了”掩盖损耗。"
            },
            {
              "itemId": "basic.sieving.mesh-repair-clamp",
              "order": 3,
              "text": "筛网修复夹把 25 点耐久恢复做成有限维护事务：玩家须核对修复材料、维护前后耐久和冷却状态；失败时保留未筛原料，重复点击不得重复恢复。"
            }
          ]
        },
        {
          "disciplineId": "materials",
          "familyId": "fire-materials",
          "familyKey": "materials.fire-materials",
          "reason": "真实 family `fire-materials`（火焰材料）把 W1 的材料属性变成选择约束：热量不是装饰数值，而是工位失败和库存回退的原因。",
          "stories": [
            {
              "itemId": "materials.fire-materials.fire-rod",
              "order": 1,
              "text": "将木棍送入冶炼锅炉获得基础热源材料；动机是先区分“能被加热的构件”和普通木材，代价是工位、燃料与等待，热工异常时回到原料而不是换用未注册替代品。"
            },
            {
              "itemId": "materials.fire-materials.fire-ingot",
              "order": 2,
              "text": "把火焰锭作为耐热装备、树脂和机器配方的材料边界；玩家必须先确认用途再消耗锭，失败教训是普通材料的短期节省会在高温工位造成更昂贵的停机。"
            },
            {
              "itemId": "materials.fire-materials.fire-block",
              "order": 3,
              "text": "用九个火焰锭压合为高热密度储块，只在持续热负载已经被实测后升级；它的代价是锁定一批可拆分性更低的耐热库存，需求未被证明时保留火焰锭比追求高阶外观更安全。"
            }
          ]
        },
        {
          "disciplineId": "materials",
          "familyId": "reinforced-thread",
          "familyKey": "materials.reinforced-thread",
          "reason": "真实 family `reinforced-thread`（强力丝线）把“材料守恒”延伸到维护与安全：同一输入如何在筛分、机器和护具之间分配，决定失败后能否继续生产。",
          "stories": [
            {
              "itemId": "materials.reinforced-thread.super-string",
              "order": 1,
              "text": "把强力丝线登记为筛网、护具与可动部件的共享韧性库存；动机是让玩家在多个工位之间规划同一稀缺输入，失败时先回收/停机而非把它当普通线材消耗。"
            },
            {
              "itemId": "materials.reinforced-thread.thread-spool",
              "order": 2,
              "text": "规划将 16 根强力丝线卷成一批，交换运输格位与批量加工效率；若该批量形态尚未可用，玩家必须继续使用 `materials.reinforced-thread.super-string` 并接受更高的搬运成本。"
            },
            {
              "itemId": "materials.reinforced-thread.thread-mesh",
              "order": 3,
              "text": "规划把韧性丝线转为胸甲的坠落缓冲，代价是牺牲可用于筛分/维护的线材库存；事故恢复应回到原始丝线和已有防护，不得把编织网当作现成保命机制。"
            }
          ]
        },
        {
          "disciplineId": "technology",
          "familyId": "machine-network",
          "familyKey": "technology.machine-network",
          "reason": "真实 P-family `machine-network`（机器网络）是“可审计供能”的叙事锚：它把能源从抽象产量变成供能/运行/停机证据；全部为规划容量，不进入当前实装门禁。",
          "stories": [
            {
              "itemId": "technology.machine-network.probe",
              "order": 1,
              "text": "规划一个针对单台设备的状态观察入口，玩家先记录供能、运行、停机三类基线再启动工位；动机是先有证据再放大产量，代价是观察时间而不是盲目开机。"
            },
            {
              "itemId": "technology.machine-network.analyzer",
              "order": 2,
              "text": "规划把状态采样变成持续比较，让玩家检查输入库存、机器状态和能源变化是否同步；若读数缺失或互相矛盾，失败路径是停机并回到实体连接检查，而不是继续向黑箱送料。"
            },
            {
              "itemId": "technology.machine-network.station",
              "order": 3,
              "text": "规划一个需供能的多机观测站，只有在单机回路稳定后才值得投入；高阶代价是额外能源和维护位，观测异常时必须隔离负载并保留最后一次有效状态，不能把自动化当成无边界运行。"
            }
          ]
        },
        {
          "disciplineId": "technology",
          "familyId": "signal-basics",
          "familyKey": "technology.signal-basics",
          "reason": "真实 P-family `signal-basics`（信号基础）把旧文明的“无界自动化”危机转成可操作的失败恢复：每次扩大控制范围，都必须先建立一个可验证的停机动作。",
          "stories": [
            {
              "itemId": "technology.signal-basics.coil",
              "order": 1,
              "text": "规划最小信号线圈，让一台负载拥有明确的开停入口；动机是先证明“能关掉”再谈自动运行，成本是导线、连接和一次人工测试。"
            },
            {
              "itemId": "technology.signal-basics.cell",
              "order": 2,
              "text": "规划可保存安全停机状态的信号单元，并要求玩家对比有信号、无信号和断电三种结果；失败教训是把供能状态与控制状态混为一谈会制造无法恢复的半运行设备。"
            },
            {
              "itemId": "technology.signal-basics.unit",
              "order": 3,
              "text": "规划面向相连机器组的安全停机单元，只有在网络观察链已经能识别异常后才升级；其代价是更大的控制范围和误停风险，当前没有该单元时统一采用断开燃料/导线与实体维护的人工回退。"
            }
          ]
        }
      ]
    },
    {
      "id": "W2",
      "order": 2,
      "title": "W2｜回到田里：水土作物的可再生生计",
      "state": "implemented",
      "disciplineIds": [
        "botany",
        "agroecology",
        "hydrology"
      ],
      "purpose": "承接 W1 的可重复加工与有限输入，把线性消耗改成可核查的水→土→作物→副产物→土回流；本波锁定守恒、来源、失败停机和恢复边界。",
      "motivation": "W1 让玩家能加工材料，却仍要不断为水、种植介质和作物输入奔波；玩家希望把第一块田升级成能持续供给并能自我修复的生计基础设施。扩大产量必须先付出水量、肥力、过滤/维护和分拣成本，玩家可从每次缺口中知道下一步该修哪里，而不是靠无限补货。",
      "crisis": "旧文明因把抽水、催熟和自动化当成无边界按钮而崩解：干旱时透支水源，污染时把不合格流体送进田块，收获后的副产物又被丢弃，最终土壤失活、管线堵塞、循环断裂。W2 的危机让一次干涸、污染、漏损或堆肥批次失败迫使系统安全停机；恢复只能使用保留下来的水、种子、残渣和维护件，不能生成水、种子或肥力。",
      "continuityIn": "W1 的 basic/materials/technology 提供工作台、材料加工、初级能量和容器边界，但其生存产出仍偏一次性消耗。W2 从这些已知输入/输出接上可观测田块状态、有限水路和副产物回流，不预设新增学科。",
      "continuityOut": "玩家交付一块有来源登记、土壤读数、过滤与灌溉分配、作物副产物回流和故障恢复记录的农区；它为 W3 defense/construction/energy 提供可审计的生物质、木材与水管理接口，但不提前实现 W3 机制。",
      "gate": "完成两条可重放的守恒行为链：有来源水→过滤→按土壤缺口灌溉→土壤复测，以及植物副产物→堆肥成熟→回田→肥力复测。注入空源、堵滤芯、污染或批次失败时，系统必须停机、保留未结算输入和失败原因；修复后从同一 batch 继续，不能无源增加水、种子或肥力。18 个故事条目只用于叙事覆盖，不参与解锁或通过计数。",
      "verificationGate": "运行行为门禁：对任意一个持久 sourceId 的有限水批次执行 source debit→过滤→按土壤缺口灌溉→土壤复测，同时将一个有来源植物副产物批次执行堆肥成熟→回田→肥力复测。注入空源或堵滤芯后支路必须停机并保留水、种苗、残渣与失败原因；修复后同一 batchId 续作且水、种子、肥力均无无源 credit。通过不要求获得或完成 18 个 anchor item。",
      "disciplineArcs": [
        {
          "id": "botany",
          "role": "把单株种植、土壤观察和植物副产物变成可追踪的生长起点；记录缺口与失败，不承诺自动增殖。",
          "whyNow": "先把作物、土壤读数和植物副产物分开，否则后续田块和水路只会把失败隐藏在产量里。",
          "input": "有来源的种子、泥土、植物掉落物和 W1 已存在的容器/工具；水和肥力只作为已登记的外部输入，不从生长步骤凭空产生。",
          "output": "可追踪的土壤/生长状态、分级作物产出、植物副产物批次与缺口信号，交给 agroecology 和 hydrology；不宣称 810 项或这些规划物品已上线。",
          "recovery": "干旱、过度施用或作物失败时暂停采收/播种，保存种苗和读数，把合格残渣转入堆肥，把不明批次隔离；缺少外部输入就回到 W1 采集/加工，不重置状态或复制产物。"
        },
        {
          "id": "agroecology",
          "role": "把单株种植编排成田块级水分、肥力、轮作与副产物回流，明确增产的代价和恢复顺序。",
          "whyNow": "botany 已能看见缺口后，必须把缺口变成灌溉和堆肥的优先级，否则所谓可再生只是更快消耗。",
          "input": "botany 的土壤、作物和残渣记录，以及 hydrology 通过质量门禁交付的有限水；田块还需保留种子、肥力和维护库存。",
          "output": "按田块分配的灌溉请求、可追溯堆肥回流和轮作/恢复状态；作物增产只能来自已登记投入与时间，不产生无来源种子或肥力。",
          "recovery": "发现干田、连续失活、污染或输水中断时隔离受影响田块，先修复水质/水路，再按缺口补水或回施成熟堆肥；失败批次留存原因并回到分拣/缓冲，不吞掉投入。"
        },
        {
          "id": "hydrology",
          "role": "把自然水、储液、过滤、泵送和故障告警约束成守恒水路，为农业生态提供可拒绝、可恢复的水。",
          "whyNow": "没有来源、容量和质量门禁，agroecology 会把免费水当作无限产量开关；W2 必须先建立水的账本和停机点。",
          "input": "可验证的自然水或储液库存、污染标记、容器/滤材/维护件与 W1 的有限能量；每次取用都要经过容量、质量和流向检查。",
          "output": "原水→过滤→田块的带来源流体记录、受限压力/容量状态、漏损/堵塞告警和可回退缓冲；不复制水、不把源头当无限再生器。",
          "recovery": "空源、堵滤芯、漏损、倒流或溢流时立即关断故障支路，保留已存液体和最后安全状态；更换维护件、清理并重新验证质量/容量后再接回，无法修复则回退到库存和手工作业。"
        }
      ],
      "familyLinks": [
        {
          "from": "botany.soil-profile",
          "to": "hydrology.source-capture",
          "kind": "supports",
          "reason": "土壤缺口生成取水需求，但 hydrology 只接受有来源和容量的请求；读数是需求信号，不是免费水权。"
        },
        {
          "from": "hydrology.source-capture",
          "to": "agroecology.irrigation",
          "kind": "supports",
          "reason": "取水口的登记水量交给灌溉分配；来源、库存和支路容量随请求传递，不能复制到多个田块。"
        },
        {
          "from": "hydrology.filtration",
          "to": "agroecology.irrigation",
          "kind": "supports",
          "reason": "只有质量通过的流体进入田路；堵滤芯或污染使灌溉停止并走恢复分支。"
        },
        {
          "from": "agroecology.irrigation",
          "to": "botany.soil-profile",
          "kind": "supports",
          "reason": "灌溉完成后回写目标田状态，下一次供水由真实干湿和肥力缺口决定，避免过灌。"
        },
        {
          "from": "botany.composting",
          "to": "agroecology.compost-loop",
          "kind": "supports",
          "reason": "botany 只交出有来源、分类完成的植物副产物批次，agroecology 负责把批次绑定到轮作田；两段不重复产肥。"
        },
        {
          "from": "agroecology.compost-loop",
          "to": "botany.soil-profile",
          "kind": "supports",
          "reason": "回田结算成为剖面可验证的肥力变化；不以堆肥完成直接宣称作物必然增产。"
        },
        {
          "from": "botany.soil-profile",
          "to": "agroecology.compost-loop",
          "kind": "supports",
          "reason": "低肥力或失败田块把可用残渣送入回流队列，恢复的是土壤状态而不是重置田块或增发种子。"
        },
        {
          "from": "agroecology.irrigation",
          "to": "hydrology.filtration",
          "kind": "supports",
          "reason": "灌溉检测到水质不确定或回流污染时把请求转为过滤任务；失败流体隔离，原有库存可重试。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "botany",
          "familyId": "soil-profile",
          "familyKey": "botany.soil-profile",
          "reason": "土壤剖面把缺水、过水和肥力下降变成玩家能行动的缺口；它连接水源与农业生态，而不是提供产量按钮。",
          "stories": [
            {
              "itemId": "botany.soil-profile.seed",
              "order": 1,
              "text": "建立第一块基线田：先记录土壤干湿、肥力与种苗来源再播种；缺水就停播，代价是等待和保留种苗，教会玩家不要把失败当随机。"
            },
            {
              "itemId": "botany.soil-profile.culture",
              "order": 2,
              "text": "将连续作业后的状态变化与培养投入绑定，比较哪块田值得补救；过度浇灌或施用会留下可见失败记录，必须减负或轮作而不是继续堆投入。"
            },
            {
              "itemId": "botany.soil-profile.greenhouse",
              "order": 3,
              "text": "在受控温室中把读数转成按缺口供水与保苗顺序；断水、传感失效或输入不足时安全停机，保留种苗和最后读数以便恢复。"
            }
          ]
        },
        {
          "disciplineId": "botany",
          "familyId": "composting",
          "familyKey": "botany.composting",
          "reason": "堆肥循环让收获后的副产物承担真实回流职责，玩家看到生计的代价不是额外装饰，而是可追溯的土壤养分来源。",
          "stories": [
            {
              "itemId": "botany.composting.reagent",
              "order": 1,
              "text": "把叶片、茎和未用植物掉落物按来源分拣入堆肥入口；玩家付出分拣与储存成本，混入非植物或污染批次直接隔离，不能把垃圾瞬间当肥力。"
            },
            {
              "itemId": "botany.composting.core",
              "order": 2,
              "text": "核心把合格残渣登记成等待成熟的肥力批次，并写清投入、损耗和预计回田地块；缺料或批次失败只留下残渣与故障原因，不复制输入。"
            },
            {
              "itemId": "botany.composting.vat",
              "order": 3,
              "text": "反应釜完成从植物副产物到可回田物料的受限转化，并把批次交给 agroecology.compost-loop；堵塞、过载或维护不足就停机，清理修复后从未完成批次继续。"
            }
          ]
        },
        {
          "disciplineId": "agroecology",
          "familyId": "irrigation",
          "familyKey": "agroecology.irrigation",
          "reason": "灌溉把水从抽象库存变成按田块缺口分配的责任；每一次供水都同时承担质量、容量和故障代价。",
          "stories": [
            {
              "itemId": "agroecology.irrigation.filter",
              "order": 1,
              "text": "田边先做水质、来源和分支标记，只放行 hydrology.filtration 已验证的水；过滤或库存不足时宁可缺水停用，代价清楚。"
            },
            {
              "itemId": "agroecology.irrigation.pump",
              "order": 2,
              "text": "把有限水提升到有土壤缺口的田块，泵送请求带目标和容量；空源、断能或回流时保留缓冲并暂停，不把抽水变成免费增产。"
            },
            {
              "itemId": "agroecology.irrigation.network",
              "order": 3,
              "text": "把多个田块接成可审计的优先级网络，按 soil-profile 读数补缺而不是全域灌溉；漏损或溢流隔离故障支路，修好后只恢复未完成配额。"
            }
          ]
        },
        {
          "disciplineId": "agroecology",
          "familyId": "compost-loop",
          "familyKey": "agroecology.compost-loop",
          "reason": "农业生态负责把植物学交出的副产物真正分配回轮作田，并以土壤复核证明回流发生，而不是把堆肥名义当成无限肥力。",
          "stories": [
            {
              "itemId": "agroecology.compost-loop.reagent",
              "order": 1,
              "text": "把 botany.composting 的合格副产物凭证接入田块循环；没有来源批次就没有肥力申请，拒绝把任何物品伪装成堆肥。"
            },
            {
              "itemId": "agroecology.compost-loop.core",
              "order": 2,
              "text": "按作物和轮作区把成熟批次分配为土壤修复输入，付出库存和成熟等待；种类不符、污染或错田块时隔离批次并回到分拣，不让失败变成额外产量。"
            },
            {
              "itemId": "agroecology.compost-loop.vat",
              "order": 3,
              "text": "完成副产物→堆肥→土壤的闭环结算，回写 botany.soil-profile 供下一轮核验；无残渣、缺水或设备维护时停机，恢复从未结算批次和保留物料开始。"
            }
          ]
        },
        {
          "disciplineId": "hydrology",
          "familyId": "source-capture",
          "familyKey": "hydrology.source-capture",
          "reason": "水源取用把“有水”改写成有来源、有容量、有维护成本的输入，直接切断免费水源和无限抽取的旧文明教训。",
          "stories": [
            {
              "itemId": "hydrology.source-capture.filter",
              "order": 1,
              "text": "在取水口拦截来源与颗粒并登记可用水量；这一步有滤材和维护代价，只允许转移被批准的存量，不能把自然源头当作无限免费水。"
            },
            {
              "itemId": "hydrology.source-capture.pump",
              "order": 2,
              "text": "把登记的有限水送入储液或田路，受容量与能量/维护预算约束；空源或倒流即关断并保留已缓存液体，恢复从库存继续。"
            },
            {
              "itemId": "hydrology.source-capture.network",
              "order": 3,
              "text": "把源头、缓冲和分配出口连成有向网络，标明每段去向与优先级；漏损或堵塞只隔离支路并报警，不静默吞水，修复后重放未完成输送。"
            }
          ]
        },
        {
          "disciplineId": "hydrology",
          "familyId": "filtration",
          "familyKey": "hydrology.filtration",
          "reason": "净水过滤把污染作为可见失败而不是隐性产量损失；质量门禁让水路、田块和恢复动作有同一份证据。",
          "stories": [
            {
              "itemId": "hydrology.filtration.filter",
              "order": 1,
              "text": "把原水或浑水与可用水分开，滤芯寿命和污染状态可追溯；滤芯堵塞时拒绝出水，避免污染进入田块。"
            },
            {
              "itemId": "hydrology.filtration.pump",
              "order": 2,
              "text": "在质量通过后以受限压力把水推向 agroecology.irrigation；压差、断能或目标满载时回退到安全缓冲，不把溢出算成成功产量。"
            },
            {
              "itemId": "hydrology.filtration.network",
              "order": 3,
              "text": "用独立的清水与待处理路径把过滤接到田块和回收点，泄漏或交叉污染自动隔离；更换滤材、清空污染批次并重新验证后才恢复供水。"
            }
          ]
        }
      ]
    },
    {
      "id": "W3",
      "order": 3,
      "title": "W3·守住火线：护身、承重与有界能源",
      "state": "implemented",
      "disciplineIds": [
        "defense",
        "construction",
        "energy"
      ],
      "purpose": "把生存安全从个人装备推进到三层基础设施：防具购买撤离时间，结构提供承重与边界证据，能源把生产绑定到燃料、损耗、容量、负载和安全停机。",
      "motivation": "玩家要把临时避难点变成能在夜袭、火场和燃料波动中继续工作的第一座 SoulTech 工坊：不是追求一件无敌装备，而是让自己、同伴、墙体和机器都知道什么时候该停、损失什么、从哪里修回来。每次成功恢复都会揭示旧文明崩解的反面答案——守住边界比榨干世界更快。",
      "crisis": "连续负载与结构缺口把“有电”变成了危险：防具耗尽、工坊过载、墙体缺口和求救信号互相放大。玩家必须在关键负载与普通生产之间取舍，先隔离、补强、复验，再逐级恢复。",
      "continuityIn": "W2 已让水、农业副产物和蒸汽拥有来源与循环：agroecology.compost-loop 提供可追溯的生物质输入，hydrology.steam-loop 与 hydrology.reservoir 让水/蒸汽有容量和回流边界。W3 接手这些可计量输入，先建立个人撤离窗口，再把它们安置进可修复结构和有保护的能源支路；materials.fire-materials 与 materials.conductive-alloy 是跨波次的既有材料接口，但不改变本波的六个 anchor 计数。",
      "continuityOut": "W3 结束时，聚落拥有能显示“正常、降载、隔离、待修复、可复工”的工作区，而不是无条件增益。W4 可用 logistics.warehouse/dispatch 管理维修件与燃料、automation.thresholds/fault-isolation 消费能源和结构状态、commerce.public-works 把公共电网与修复投入纳入可审计协作；任何自动化都只能在 W3 的负载边界和手动恢复路径上继续。",
      "gate": "W3 生存基础设施门禁：完成一次可恢复能源故障演练。将 energy.solid-fuel.unit 接到 energy.grid-protection.unit 保护的支路上，在不固定最终 tick/比例的前提下制造“持续超过额定输入”的超载；保护机组必须隔离故障支路、保留关键负载并显示可读原因，不能静默损坏或无限重试。玩家随后关闭非关键设备，使用 technology.electric-components.wrench 检查/维护，清除过载源并补充 solid-fuel 输入，手动复位保护后逐级恢复关键工坊；验证未丢失未完成输入、停机原因和维修状态仍可追溯。最后移除一处 construction.modular-wall 的受损模块，确认相邻结构/维修坐标仍存在并在补回墙体与复检承重后恢复工作。该门禁只证明“故障可隔离且可复工”，不把防具、蓄能或能源产出当作无代价永久增益。",
      "verificationGate": "运行行为门禁：用有限燃料和 no-voltage milli-SE 电网运行一个受保护工坊支路，注入超过共享通量/负载容量的故障；`energy.grid-protection` 必须隔离非关键支路、保留关键负载和未完成输入。随后修复一处结构缺口、清除过载、人工复验并逐级复位，同一 workId 只结算一次。观察项是通量、损耗、隔离原因、结构坐标和恢复状态，不是装备或设施收集数。",
      "disciplineArcs": [
        {
          "id": "defense",
          "role": "把生存风险从即时伤害翻译成有限的个人缓冲、撤离路径和求救信号，先教玩家为什么需要基础设施。",
          "whyNow": "W2 已提供可持续的材料/水/农业输入，玩家开始离开临时田地和水源；在扩建之前必须先知道护具只能买时间，否则后面的建筑和能源会建立在错误的“无敌”预期上。",
          "input": "个人先从 materials.fire-materials、materials.reinforced-thread 和基础工具获得有限耐热、机动与求救材料；实际链路以 defense.heat-armor 与 defense.mobility-armor 的六件目录物品为锚，不把现有 implemented 标记解释为 runtime 全量上线。",
          "output": "玩家得到有明确覆盖、触发条件、持续时间/承载与维护成本的撤离窗口：防火、跳跃、单次落地缓冲和危险告警都能改变决策，却不能永久免伤、无限位移或自动救援。防具故障会把玩家转回 construction.scaffold、construction.modular-wall 与 exploration.rescue-beacon 的结构/协作路径。",
          "recovery": "先离开危险源并读取告警，再更换磨损件、补充材料或重新激活设施；若逃生窗口已用尽，状态必须保留为“待维护/待救援”，不能用重穿装备清空后果。恢复成功的判据是玩家能沿可见路线回到结构安全区，而不是获得永久 buff。"
        },
        {
          "id": "construction",
          "role": "把个人防护升级为城邦的承重、遮蔽、照明与可维护边界，让玩家看见安全来自结构证据而非装饰数量。",
          "whyNow": "护具已证明撤离窗口有限；一旦玩家要长期驻留、多人共享或让机器连续作业，任何缺口都必须有位置、有责任、有恢复顺序。",
          "input": "承接 defense 的撤离路线与 materials 的原木、铁锭、圆石、石砖、木板、树脂，先装配 construction.structural-frame，再用 construction.modular-wall 形成可替换边界；工坊启用还要检查受限能源。",
          "output": "玩家得到可观察的承重链、维修坐标、分区边界和会在结构异常时停机的工坊。结构安全不是装饰判定：连接/基础/跨度不足时拒绝安全确认，墙体破坏留下局部缺口和修复队列，设施不会在危险状态继续产出。",
          "recovery": "结构故障按“隔离危险方向→标记缺口→补砖/框架→复查承重→恢复工坊”的顺序处理；断电时保留结构状态和维修坐标，电力恢复不能跳过结构复检。"
        },
        {
          "id": "energy",
          "role": "把能源从“更大数字”变成可调度的守恒网络，教玩家接受燃料消耗、线路损耗、容量上限和可恢复跳闸。",
          "whyNow": "结构工坊和防御设施一旦启用就会形成持续负载；如果没有燃料账本和故障隔离，W3 仍会复演旧文明的无限抽取与跨域级联失控。",
          "input": "承接 technology.thermal-generation、technology.energy-storage、technology.electric-components 的既有接口，以及 agroecology.compost-loop、hydrology.steam-loop 的可计量输入；本波重点是 energy.solid-fuel 和 energy.grid-protection 两条六件物品链。",
          "output": "玩家获得“输入→转换→储存/传输→负载→损耗→停机→恢复”的有界能源心智模型。固体燃料明确消耗与余量，保护支路明确额定与隔离；非关键机器可以被降载，关键支路可以保留，但不会凭空增加总能量。",
          "recovery": "故障先由 energy.grid-protection 隔离并保留原因，再停非关键负载、用 technology.electric-components.wrench 检查维护、补燃料/修结构、确认负载优先级，最后逐级手动复位。恢复必须能在不重复消耗未完成输入的前提下继续作业。"
        }
      ],
      "familyLinks": [
        {
          "from": "materials.fire-materials",
          "to": "defense.heat-armor",
          "kind": "supports",
          "reason": "火焰锭/火焰块是耐火护甲和后续耐热设施的真实材料入口；材料稀缺让防火窗口有机会成本。"
        },
        {
          "from": "defense.mobility-armor",
          "to": "construction.scaffold",
          "kind": "supports",
          "reason": "跳跃靴和落地板只能降低一次移动风险，必须把玩家导向可回程的高处施工路径。"
        },
        {
          "from": "defense.mobility-armor",
          "to": "exploration.rescue-beacon",
          "kind": "supports",
          "reason": "撤离信标只产生本地危险事实，W4/W3 后续救援网络需要把它接到远距救援节点。"
        },
        {
          "from": "defense.heat-armor",
          "to": "construction.modular-wall",
          "kind": "supports",
          "reason": "烈焰壁垒的有限覆盖不能代替墙体；火源/袭击仍需由可替换边界承接。"
        },
        {
          "from": "construction.structural-frame",
          "to": "construction.modular-wall",
          "kind": "supports",
          "reason": "先证明承重链，再允许墙面和工坊宣告安全，避免装饰面板掩盖基础断点。"
        },
        {
          "from": "construction.structural-frame",
          "to": "energy.solid-fuel",
          "kind": "supports",
          "reason": "结构工坊把承重检查变成持续作业，必须消耗受限电源并在断电或结构破坏时停机。"
        },
        {
          "from": "agroecology.compost-loop",
          "to": "energy.biomass",
          "kind": "supports",
          "reason": "W2 的作物副产物进入低速能源路线，形成“农业废料有去向但产能不超额”的循环。"
        },
        {
          "from": "hydrology.steam-loop",
          "to": "energy.steam-turbine",
          "kind": "supports",
          "reason": "水利的蒸汽输出成为涡轮的受限输入，能源产出受水、热和输送容量共同约束。"
        },
        {
          "from": "energy.solid-fuel",
          "to": "energy.grid-protection",
          "kind": "supports",
          "reason": "所有燃料机组必须先经过支路额定、状态读数和保护策略，燃料投入不能绕过电网边界。"
        },
        {
          "from": "energy.grid-protection",
          "to": "automation.fault-isolation",
          "kind": "supports",
          "reason": "W3 产生可定位的隔离/停机事件，W4 只能消费它并等待根因解决，不能无条件自动重启。"
        },
        {
          "from": "construction.modular-wall",
          "to": "logistics.warehouse",
          "kind": "supports",
          "reason": "墙体修复需要预留、分类和返还材料；物流满载时应停止投料而不是吞掉维修件。"
        },
        {
          "from": "energy.load-response",
          "to": "commerce.public-works",
          "kind": "supports",
          "reason": "能源不足时的优先级停机为公共工程保留关键负载，后续公共建设才能公开推进而非争抢电量。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "defense",
          "familyId": "heat-armor",
          "familyKey": "defense.heat-armor",
          "reason": "耐火护甲（耐火护甲）是生存基础设施的第一道、也是最有限的一道缓冲：让玩家理解装备保护的是撤离时间，并以磨损、材料和覆盖范围支付代价。",
          "stories": [
            {
              "itemId": "defense.heat-armor.fire-chestplate",
              "order": 1,
              "text": "输入 materials.fire-materials 的火焰材料与护甲槽，给玩家一段可穿越炉区/火场的短时缓冲；代价是消耗稀缺耐热材料并占用装备位，抗火不等于免疫其他伤害，热负荷或磨损达到边界就必须撤离维护。"
            },
            {
              "itemId": "defense.heat-armor.fire-greaves",
              "order": 2,
              "text": "输入火焰锭、铁质护腿与皮革，把“岩浆接触伤害延后”变成逃生倒计时，而不是取消伤害；若玩家没有 construction.scaffold 或 construction.bridgework 的可回程路线，延迟只会把失败推迟，故损耗后要回收、检修或换件。"
            },
            {
              "itemId": "defense.heat-armor.fire-bastion",
              "order": 3,
              "text": "输入火焰块、黑曜石与红石控制，给据点半径内提供短时集体抗火窗口；壁垒需要激活、能量/维护预算和明确覆盖边界，失效时先告警再撤离。故事教训是把个人抗性扩大成永久领域会掩盖火源和电网风险，壁垒必须服务于 evacuation，而不是替代安全结构。"
            }
          ]
        },
        {
          "disciplineId": "defense",
          "familyId": "mobility-armor",
          "familyKey": "defense.mobility-armor",
          "reason": "机动防具把个人逃生从蛮力升级为路线纪律：跳过障碍、承受一次冲击、发出求救，但每一步都把玩家推回可维护的结构和救援网络。",
          "stories": [
            {
              "itemId": "defense.mobility-armor.jumper-boots",
              "order": 1,
              "text": "输入羽毛、树脂与火焰块，允许玩家越过短缺口，输出的是一次可控位移窗口而非永久飞行；位移需要冷却/耐久和落点判断，失败时仍会坠落，因此玩家必须把路线写进结构规划。"
            },
            {
              "itemId": "defense.mobility-armor.landing-plate",
              "order": 2,
              "text": "输入树脂、强力丝线与铁锭，将一次高处坠落转成有限的伤害缓冲；缓冲只对单次冲击、且有承载损耗，不能连续堆叠成无代价免伤。触发后要回到工作区替换或维护，并检查 construction.scaffold 的支撑链。"
            },
            {
              "itemId": "defense.mobility-armor.escape-beacon",
              "order": 3,
              "text": "输入红石灯、铁锭与末影珍珠，在生命值进入危险状态时发出粒子与钟声，输出可定位的求救事实而不是传送；信标有电量/激活时限和可达路线要求，若只会报警而没有 construction.modular-wall、construction.scaffold 或 exploration.rescue-beacon 接应，失败教训就是“预警不等于救援”。"
            }
          ]
        },
        {
          "disciplineId": "construction",
          "familyId": "structural-frame",
          "familyKey": "construction.structural-frame",
          "reason": "结构框架把防御从穿在身上的效果迁移到脚下的承重证据，建立“材料投入→连接验证→设施停机→修复复工”的空间安全主线。",
          "stories": [
            {
              "itemId": "construction.structural-frame.brick",
              "order": 1,
              "text": "输入原木、铁锭与圆石，把“我要盖房子”改写为可检查的承重材料层；每一块材料都被锁定在结构用途，不能同时算作装饰或无限承重，缺料时施工应停在可识别的半成品状态。"
            },
            {
              "itemId": "construction.structural-frame.frame",
              "order": 2,
              "text": "输入同系列结构材料，只有形成连续支撑链才输出承重确认；跨度、连接和基础不满足时拒绝宣告安全，而不是给玩家隐形增益。断裂后保留可修复的支撑状态并标出缺口，玩家补回框架、复查路线后才能继续作业。"
            },
            {
              "itemId": "construction.structural-frame.workshop",
              "order": 3,
              "text": "输入完整框架、工坊组件与受保护电源，把承重检查变成可持续的工作站；工坊运行有供能、维护和占地成本，结构被破坏或电源停机时暂停，不应继续产出“安全”标记。恢复顺序是先补承重链，再恢复能源，最后重新启用工坊。"
            }
          ]
        },
        {
          "disciplineId": "construction",
          "familyId": "modular-wall",
          "familyKey": "construction.modular-wall",
          "reason": "模块墙体将防护落实为可替换、可定位、会停机的边界；它承接耐火壁垒的短时窗口，也为能源事故提供隔离和复工空间。",
          "stories": [
            {
              "itemId": "construction.modular-wall.brick",
              "order": 1,
              "text": "输入石砖、木板与树脂，建立可替换的空间边界和避火/避袭击分区；墙体是有寿命的模块，受损后消耗维修材料并留下缺口，不能把一圈方块当成永久防线。"
            },
            {
              "itemId": "construction.modular-wall.frame",
              "order": 2,
              "text": "输入墙板与结构连接件，使墙体破坏时相邻结构框架保持可诊断状态并显示维修坐标；代价是面板与支撑必须分别维护，替换一块墙不能掩盖承重断点。若坐标未修复，相关工位应暂停或降低安全等级。"
            },
            {
              "itemId": "construction.modular-wall.workshop",
              "order": 3,
              "text": "输入模块墙、维护件与受限能源，把墙体裂口转成维修队列；工坊只在边界、结构与电源都正常时运行，断电或材料耗尽时停机并保留维修位置。恢复要先封闭危险方向、补墙板/框架，再复位工坊，避免“自动修墙”成为无限资源和永久防护。"
            }
          ]
        },
        {
          "disciplineId": "energy",
          "familyId": "solid-fuel",
          "familyKey": "energy.solid-fuel",
          "reason": "固体燃料把旧文明的无限抽取问题压缩成一条可审计账本：燃料是输入，热损与磨损是代价，容量与停机是边界，恢复是玩家必须掌握的生产技能。",
          "stories": [
            {
              "itemId": "energy.solid-fuel.coil",
              "order": 1,
              "text": "输入煤炭/木炭等固体燃料与铁锭，输出可计量的热能或电能入口，并记录燃料剩余；每一点输出都对应燃料消耗、热损和线圈磨损，燃尽必须停机，禁止把能源写成永久被动加成。"
            },
            {
              "itemId": "energy.solid-fuel.cell",
              "order": 2,
              "text": "输入线圈产出的受控能量与储能材料，输出短时缓冲以抹平燃料波动；容量有限且存在待机损耗/老化，储能只能搬移时间不能创造能量。读数低、漏损或温度越界时先降载，维护后才能重新充入。"
            },
            {
              "itemId": "energy.solid-fuel.unit",
              "order": 3,
              "text": "输入燃料、机组组件和有额定边界的电网连接，输出可调度的聚落能源；燃料不足、输出端满载、机组过热或结构/保护状态异常都必须给出原因并安全停机。恢复路径是补充燃料、处理热/结构警报、确认负载优先级，再逐级重启，不得静默吞掉未完成输入。"
            }
          ]
        },
        {
          "disciplineId": "energy",
          "familyId": "grid-protection",
          "familyKey": "energy.grid-protection",
          "reason": "电网保护是 W3 的“失败可恢复”核心：它承认能源会超载、会中断、会牺牲非关键任务，但把故障隔离成可定位、可维护、可复工的事件。",
          "stories": [
            {
              "itemId": "energy.grid-protection.coil",
              "order": 1,
              "text": "输入一段带额定值的导线、检测信号与维护材料，输出该支路的状态/限流证据；保护件本身有监测容量和损耗，不能只在故障后凭空修复，读数不可用时应把支路标为未知并拒绝扩载。"
            },
            {
              "itemId": "energy.grid-protection.cell",
              "order": 2,
              "text": "输入保护状态、有限备用电量与电路板，输出一次可完成告警和有序停机的缓冲；备用电不是生产电，耗尽后只保留故障记录。它把过载支路与关键负载分开，代价是非关键工位会暂停、未完成作业必须进入可恢复状态。"
            },
            {
              "itemId": "energy.grid-protection.unit",
              "order": 3,
              "text": "输入完整支路额定信息、保护组件和红石控制，检测到持续超载时隔离故障支路并保留关键支路/告警；跳闸会造成真实停机和复位成本，不能自动把危险电流重新接回去。玩家用 technology.electric-components.wrench 检查原因、移除过载、维护或替换保护件后，才可手动复位并逐步恢复。"
            }
          ]
        }
      ]
    },
    {
      "id": "W4",
      "order": 4,
      "title": "W4 · 有界协同生产：让物资回家",
      "state": "implemented",
      "disciplineIds": [
        "logistics",
        "automation",
        "commerce"
      ],
      "purpose": "把已供能、受保护的加工岛连接成可见、可暂停、可回收的协同网络：物流搬运有来源和回程的批次，自动控制只在证据与资源边界内决策，聚落经济记录私人委托与公共工程。",
      "motivation": "玩家要重建的不是更快的黑箱，而是一条多人仍敢托付材料的生产链：看见自己的货从 W3 产出走到哪，知道它为什么停，修复后能取回未完成投入，并在公共工程和私人订单之间承担真实选择。每次扩容都会暴露容量、能量、路线和材料代价，效率不再等于无边界抽取。",
      "crisis": "跨站批次在满载、错配、断线和区块暂停时失去去向；若把送出当完成，库存会出现无主货物，自动重试还会重复扣料。玩家必须让停机、退货、合约取消和公共储备各有记录。",
      "continuityIn": "W3 留下了有能量预算、结构保护和加工状态的生产节点，但只证明单点可以安全运行，尚未解决跨节点可见流动、机器决策边界或多人承诺。W4 必须从 W3 已产出的真实材料和有限电力开始，不创造前段输入，不把未来的经济设施写成当前生产能力。",
      "continuityOut": "W4 交付一条可审计协作链：每件物品有来源、去向和回程，机器能停机、隔离并从检查点恢复，订单与公共工程都有贡献、履约、退回和归属记录。W5 可以把这套守恒路由当作矿物、构件和合金的输入边界，但仍必须经过勘探、分选、冶炼和装配，不能由自动化直达。",
      "gate": "运行验收门槛：使用 W3 已有的有限材料与能量搭一条跨三台加工机的链路，同时运行一笔私人合约和一项公共工程。必须能从 logistics.belt-line 看见来源到目的地并在停滞时触发 logistics.jam-alarm；错配或满载经 logistics.return-route 原路保留/退回；automation.thresholds 在输入、输出、能量或路线任一缺失时停投，automation.fault-isolation 隔离故障并由 automation.recovery 无重复地续作；commerce.work-contract 只有交付证据成立才结算，commerce.cargo-insurance 在运输失败时保留原归属，commerce.public-works 与私人收益的分账、延期和公共回收可审计。全程保持物品守恒、有限并发和前段材料/能源依赖，不出现一步直达或无限免费资源；通过后才把这条协作链交给 W5。",
      "verificationGate": "运行行为门禁：让一个有 owner、recipe、source/target 与公共储备约束的批次经过至少一个多步骤机器链和一项合约结算；注入满载、错配或区块暂停，必须停止新投料、触发可定位告警、把未承诺物留在 escrow/return-route，并以持久且有限 attempt 从 checkpoint 续作。最终验证同一物品总量与唯一 owner 不变、输入/产出各结算一次、私人任务不能越过公共储备底线；不按机器或物品收集数量判定。",
      "disciplineArcs": [
        {
          "id": "logistics",
          "role": "可见流动层：让每件物品拥有来源、去向、优先级、容量边界与回程，而不是让输送成为不可解释的黑箱。",
          "whyNow": "W3 的机器已经能产出，但没有跨节点路线时产出只会堆在错误容器；在自动控制和多人经济接入前，玩家必须先能看见货物为何移动、为何停下以及如何回家。",
          "input": "W3 加工节点的真实材料、带来源标记的容器、路线段、目的地容量、优先级和回程标识；运输本身也受有限电力与通道容量约束。",
          "output": "可见的源点—中转—目的地路线、批次状态、容量停机、堵塞告警、库存移动记录和拒收回程；物流只改变位置与可见状态，不改变物品总量。",
          "recovery": "满载、错配、断线或区块暂不可用时先停上游并保留批次，触发堵塞告警，把拒收物送入标记退货容器；修复容量/连接或重新加载区块后按账本续接一次，不丢弃、不复制、不把未送达标成完成。"
        },
        {
          "id": "automation",
          "role": "有界决策层：用传感、阈值、配方检查和故障隔离编排机器，而不是用重试掩盖资源不足。",
          "whyNow": "只有物流把容量和去向变成可观察事实后，自动化才有安全决策输入；否则一条“自动生产”命令会复演旧文明的无限抽取与无界重试。",
          "input": "物流路线状态、库存/能量/输出空间读数、当前配方白名单、授权任务、任务检查点，以及公共储备底线；不接受凭空出现的输入。",
          "output": "有界的启动、等待、取消和故障隔离决策；每次只准一个符合条件的任务占用容量，缺输入、能量、出口或路线时先停机并说明原因，自动控制不产资源、不跨过前段加工。",
          "recovery": "遇到低库存、路线堵塞、配方错配、重复异常或区块重载时切断新投料、隔离故障支路并保存检查点；修复后只恢复能证明尚未结算的任务，无法证明就保持停机。"
        },
        {
          "id": "commerce",
          "role": "多人承诺与回收层：把个人库存、工坊库存和公共储备分开，让收益必须以实际履约为前提。",
          "whyNow": "当流动可追踪、决策可停止后，玩家才有证据把材料交给别人；否则合约只是把隐藏损失和公共资源争抢包装成“效率”。",
          "input": "工坊合约的输入/输出/期限/所有者/受益人、真实库存或暂存物、路由交付证据、参与者授权、公共储备和工程目标；价格可以遵循服主外部政策，不假定本插件已有官方货币。",
          "output": "可接受、执行、交付、取消、过期和退回的多人承诺状态；信誉与交付原因记录、运输归属回收、公共工程进度和私人收益分账都能被参与者核对。",
          "recovery": "路由失败、机器隔离或订单过期时，未消耗输入回到原所有者/原容器并记录原因，货运保障与退货路线不能凭空增发补偿；公共库存不得静默转给私人订单，争议进入可见的取消、补交或延期状态。"
        }
      ],
      "familyLinks": [
        {
          "from": "logistics.belt-line",
          "to": "logistics.return-route",
          "kind": "supports",
          "reason": "每段可见输送都必须有拒收回程；错配批次进入标记退货，不让重试把单点堵塞扩大。"
        },
        {
          "from": "logistics.belt-line",
          "to": "automation.thresholds",
          "kind": "supports",
          "reason": "路段容量和下游接收状态是自动控制的前置证据；没有接收空间就停在上游，不继续抽取。"
        },
        {
          "from": "logistics.jam-alarm",
          "to": "automation.fault-isolation",
          "kind": "supports",
          "reason": "告警先指向具体停滞节点，自动控制隔离该支路而非全网重试，并保留故障原因。"
        },
        {
          "from": "automation.thresholds",
          "to": "automation.fault-isolation",
          "kind": "supports",
          "reason": "阈值失守或配方、输出不符时从开机切到隔离，边界内停止新投料。"
        },
        {
          "from": "automation.fault-isolation",
          "to": "automation.recovery",
          "kind": "supports",
          "reason": "隔离分支留下检查点；区块重载或维修后只恢复未完成任务，禁止重复扣料。"
        },
        {
          "from": "automation.recovery",
          "to": "logistics.return-route",
          "kind": "supports",
          "reason": "恢复前先清点待处理与拒收批次；未被承诺的输入回到退货路线，避免重启制造重复物。"
        },
        {
          "from": "logistics.return-route",
          "to": "commerce.work-contract",
          "kind": "supports",
          "reason": "退货事件使订单保持未履约或可补交，不把退回物误记成交付。"
        },
        {
          "from": "commerce.work-contract",
          "to": "commerce.cargo-insurance",
          "kind": "supports",
          "reason": "订单货物绑定原所有者与原容器；运输失败通过货运保障回收并记录，而不是凭空赔付。"
        },
        {
          "from": "commerce.work-contract",
          "to": "commerce.public-works",
          "kind": "supports",
          "reason": "公共工程先锁定公共材料和贡献证据；私人奖励只能使用明确剩余，直接处理公共投入与私人收益冲突。"
        },
        {
          "from": "commerce.public-works",
          "to": "logistics.belt-line",
          "kind": "supports",
          "reason": "公共材料使用独立标签和路线进入工程站，不能被私人优先级静默抢走；容量不足必须公开延期。"
        },
        {
          "from": "commerce.public-works",
          "to": "automation.thresholds",
          "kind": "supports",
          "reason": "公共储备底线是自动任务的开机条件；底线受威胁时暂停私人工单，而不是绕过前段资源。"
        },
        {
          "from": "commerce.cargo-insurance",
          "to": "commerce.reputation",
          "kind": "supports",
          "reason": "失败交付与主动取消分开记录，保障回收不能被错误计为恶意违约，信誉才反映真实承诺。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "logistics",
          "familyId": "belt-line",
          "familyKey": "logistics.belt-line",
          "reason": "可见流动的主线锚点：把“搬运效率”改成可观察的路线、容量与停机选择，暴露旧文明无限抽取却不为目的地负责的错误。",
          "stories": [
            {
              "itemId": "logistics.belt-line.tag",
              "order": 1,
              "text": "复原者先为每段物料写明来源与下一站；第一次运输不是加速，而是让玩家能回答“这件东西从哪来、要去哪”。标签缺失时留在原容器，不进入不可解释的黑箱。"
            },
            {
              "itemId": "logistics.belt-line.sorter",
              "order": 2,
              "text": "分拣前先确认目标槽位和下游容量；满载或错配就停在可见缓冲，不把库存继续抽空。玩家学到速度必须服从容量，不能用分拣器绕过前段供给。"
            },
            {
              "itemId": "logistics.belt-line.relay",
              "order": 3,
              "text": "跨站中继只转发有路线、限额和回程的批次；断线、缺站或区块未加载时挂起并留下记录。旧文明把“送出即算完成”当效率，复原者证明它其实制造了无主货物。"
            }
          ]
        },
        {
          "disciplineId": "logistics",
          "familyId": "return-route",
          "familyKey": "logistics.return-route",
          "reason": "失败恢复的物流锚点：让拒收、堵塞和中断都有回程，教玩家用可追踪退货修复生产链，而不是用更大的抽取量掩盖损失。",
          "stories": [
            {
              "itemId": "logistics.return-route.tag",
              "order": 1,
              "text": "为拒收口指定唯一退货容器，并把错配物连同来源标签送回；失败意味着待处理，不意味着删除或吞掉投入。"
            },
            {
              "itemId": "logistics.return-route.sorter",
              "order": 2,
              "text": "将正常出货和退货路线分离；退回批次进入人工复核或新订单，不再重投原拒收口，避免自动重试把单点堵塞扩成循环堵塞。"
            },
            {
              "itemId": "logistics.return-route.relay",
              "order": 3,
              "text": "连接修复或区块重载后，按账本重新放行仍属于原所有者、尚未交付的批次；恢复是续接未完成运输，不是复制物品或伪造交付。"
            }
          ]
        },
        {
          "disciplineId": "automation",
          "familyId": "thresholds",
          "familyKey": "automation.thresholds",
          "reason": "有界决策的主线锚点：自动化只在输入、能量、输出和路由证据同时成立时开机，永远不生成资源，也不绕过前段材料处理。",
          "stories": [
            {
              "itemId": "automation.thresholds.part",
              "order": 1,
              "text": "只有当前段输入、目标配方、输出空间和能量都可验证，才允许一次作业；缺任一项先待机，安全停止比吞料更有价值。"
            },
            {
              "itemId": "automation.thresholds.drive",
              "order": 2,
              "text": "把启动/停机上下限和优先级写成可读策略；任务只能取得受容量约束的时间片，不能因为队列存在就抽空公共仓或个人库存。"
            },
            {
              "itemId": "automation.thresholds.workstation",
              "order": 3,
              "text": "跨三机链在任一边界失守时冻结该链并保留检查点，待材料、能源和通道恢复再续作；无限并发被明确判为旧文明崩解的复演。"
            }
          ]
        },
        {
          "disciplineId": "automation",
          "familyId": "fault-isolation",
          "familyKey": "automation.fault-isolation",
          "reason": "故障恢复的自动控制锚点：把异常局部化，阻止一台坏机把物流和多人订单拖入无界重试，并将恢复建立在可验证状态上。",
          "stories": [
            {
              "itemId": "automation.fault-isolation.part",
              "order": 1,
              "text": "检测到输入错配或设备异常时，先切断新输入并标记机器，不清空故障槽；失败原因必须比假成功更可见。"
            },
            {
              "itemId": "automation.fault-isolation.drive",
              "order": 2,
              "text": "将故障支路与健康支路分开，未受影响的任务继续；异常事件回指具体路由或订单，使被卡住的材料和责任可追踪。"
            },
            {
              "itemId": "automation.fault-isolation.workstation",
              "order": 3,
              "text": "维修或区块重载后按检查点恢复未完成作业一次；已结算输入不再次扣除，无法证明状态时保持停机，宁可少产出也不制造幽灵货物。"
            }
          ]
        },
        {
          "disciplineId": "commerce",
          "familyId": "work-contract",
          "familyKey": "commerce.work-contract",
          "reason": "多人承诺的经济锚点：把口头协作变成有输入、期限、交付证据和回收路径的可撤销承诺，避免旧文明用虚假订单驱动无限生产。",
          "stories": [
            {
              "itemId": "commerce.work-contract.token",
              "order": 1,
              "text": "一份承诺先标明输入、输出、期限、所有者、受益人及公共/私人属性；没有清楚的归属，任何“帮忙生产”都不能算订单。"
            },
            {
              "itemId": "commerce.work-contract.contract",
              "order": 2,
              "text": "只有输入真实锁定且路线、机器容量与能量可用，合约才进入执行；过期、取消或退货只留下未履约记录，不提前发奖励、不把承诺当产出。"
            },
            {
              "itemId": "commerce.work-contract.exchange",
              "order": 3,
              "text": "仅在库存账本和交付证据都成立后结算；运输失败交由货运保障回收原物并记录原因，信誉区分基础设施故障、主动取消和恶意不履约，收益来自真实库存而非凭空铸造。"
            }
          ]
        },
        {
          "disciplineId": "commerce",
          "familyId": "public-works",
          "familyKey": "commerce.public-works",
          "reason": "公共工程冲突锚点：让共享基础设施的成本、贡献和私人收益同屏可见，直接暴露旧文明把公共抽取私有化的失败。",
          "stories": [
            {
              "itemId": "commerce.public-works.brick",
              "order": 1,
              "text": "公共工程以独立标签接收实际贡献，公共材料进入公共储备，个人库存仍归个人；口头捐赠或未来收益不能冒充已交材料。"
            },
            {
              "itemId": "commerce.public-works.frame",
              "order": 2,
              "text": "进度只认公开的贡献与路线证据；私人合约不能静默抽走公共储备，若材料冲突就显示延期和原因，而不是让高价订单抢占城墙或水渠。"
            },
            {
              "itemId": "commerce.public-works.workshop",
              "order": 3,
              "text": "公共工程完成后先开放共享产能并补足维护储备，剩余收益才按合约分配；贡献者、受益者和公共回收都可审计，公共能力不变成少数人的无限免费吞吐。"
            }
          ]
        }
      ]
    },
    {
      "id": "W5",
      "order": 5,
      "title": "W5·深层材料：证据成矿，性质成器",
      "state": "implemented",
      "disciplineIds": [
        "geology",
        "metallurgy",
        "mechanics"
      ],
      "purpose": "把深层资源从挖到即得改成可解释、可回退的材料主线：地质证据决定是否值得开采，冶金分离与控温决定性质，机械把合格批次转成有负载边界、可检修的能力。",
      "motivation": "W4 交来的工坊订单暴露了核心材料缺口；旧文明遗留矿带看似能直达终局，却没有稳定证据。玩家要在有限样本、过程介质、时间和耐久下先证明矿脉，再拿到可验证的材料性质，最后建成一条不会因一次磨损就重摆的生产线；做对比“挖更多”更可靠，做错也能看出应回到勘探、分选、熔炼还是维护。",
      "crisis": "旧文明把矿带当作无限箱子，未经分选的混料直接进入高热炉，未经验证的材料又被无界自动化装进跨域设备，最终造成矿井坍塌、炉批失性和设备无法修复。复原者面对同一条深层矿带：稀有材料、断层与污染并存，缺证据就开挖会同时耗掉样本、结构和维护储备，必须用证据、属性与维修闭环重建可持续城邦。",
      "continuityIn": "W4 的 `logistics.inventory-ledger`、`automation.recipe-control`、`commerce.work-contract` 已经把需求、批次去向和交付条件写清；W5 从一条带来源标签的材料需求与库存短缺开始，不接受“任意矿石都能喂给终局配方”的隐式捷径。当前生产端仍只有 150 件，27 学科与 810 条物品是 `catalog.js` 的规划容量，W5 不应被描述为已落地功能。",
      "continuityOut": "W5 输出带勘探来源、分选结果、热处理历史、性质证书和维修记录的可追溯材料批次，并把可用金属、残渣、损坏件分别送往明确出口；后续 `chemistry`、`environment`、`exploration` 可以接手反应、环境与远征语义，但不得重新发明材料来源、属性或维护状态。",
      "gate": "门禁是一次可重放且可观察的 `geology.ore-prospecting.station → metallurgy.ore-washing.vat → metallurgy.smelting-control.vat → mechanics.drive-shaft.workstation → mechanics.maintenance.station → metallurgy.recycling.workstation` 闭环。界面与日志必须展示同一批次的 source ID、输入、净料、残渣/返工/拒收、性质证书、维修前后状态和回收去向；无勘探证据时分选拒收，无性质证书时机械装配拒收，磨损或故障触发安全停机。拆下件回收后必须重新经过分选/熔炼与认证才能回装，恢复作业时同一输入只结算一次。该门禁只验收规划契约的可观察性，不把 810 条策划物品或任何 W5 家族宣称为当前生产功能。",
      "verificationGate": "运行行为门禁：选取任意一个带 sourceId 的批准原矿批次，依次产生勘探 proof、分选守恒账、热史/性质证书和机械装配记录；注入无证据输入、混批或磨损，入口必须拒收/停机且不预扣不可恢复材料。维护后把拆件送回收，回收物以 returns 重新进入分选而不绕过认证，并从 checkpoint 完成原工单一次。门禁看完整 batch lineage，不看采集了多少矿或物品。",
      "disciplineArcs": [
        {
          "id": "geology",
          "role": "证据闸门：把地层与矿脉读数转为可批准的开采区段，不直接生成终局材料。",
          "whyNow": "W4 的订单已经暴露深层材料缺口，而旧文明崩溃的首因正是把未知矿带当无限资源；先证明方向、连续性和风险，才值得让后续工位消耗水、燃料、时间与耐久。",
          "input": "W4 带来源的材料需求、岩层/矿石样本，以及 `geology.strata-survey.*` 和 `geology.ore-prospecting.*` 的观测结果；样本不足时输入仍只是证据，不会自动变成矿批。",
          "output": "可复核的勘探记录（坐标、岩层、候选矿脉、风险、置信度、批准走廊）与带来源标签的待分选原矿，分别交给 `metallurgy.ore-washing`；无证据只得到待补采状态。",
          "recovery": "读数冲突、断层或空洞出现时保留已采样证据，撤销批准走廊而不是吞掉样本；玩家可按记录补测或换路径，再重新提交，失败不生成终局材料。"
        },
        {
          "id": "metallurgy",
          "role": "性质闸门：将带来源的原矿分离、熔炼并认证为用途明确的材料，控制成分、热史和副产物。",
          "whyNow": "地质证据只能说明“哪里值得挖”，不能保证原矿可用；如果没有分选与控温，机械拿到的就是不可预测的软、脆或污染批次，会重演旧文明的跨域失控。",
          "input": "批准的原矿批次、来源记录，以及由 `metallurgy.ore-washing` 和 `metallurgy.smelting-control` 指定的水、热、冷却、燃料等过程介质；无来源或混批输入必须拒收。",
          "output": "分级净料、残渣/返洗料、带热历史和性质证书的合格金属坯或构件；每份输入在台账中落到产出、残渣、返工、拒收或未处理库存，不能隐藏损耗。",
          "recovery": "分选不达标就回洗或留在拒收槽；控温或冷却失配就安全停炉，保留可重熔坯、残渣和失败原因，不随机重抽属性；返工完成后沿同一来源链继续。"
        },
        {
          "id": "mechanics",
          "role": "能力闸门：把性质受控的材料转成带负载、磨损和维护边界的机械能力。",
          "whyNow": "合格金属只有装进动力轴和作业台、并能在磨损后修复，才是城邦能力；无界自动化的教训要求负载、停机、维修和回收都可见。",
          "input": "冶金合格批次与性质证书、结构/动力需求和维护件库存；`mechanics.drive-shaft` 不接受未认证原矿，`mechanics.maintenance` 必须先读状态再开工。",
          "output": "具有负载边界、维护计数、输入输出检查和来源链接的动力轴/作业单元；运行中的磨损件、合格件和损坏件分别导向维护、回收或再加工。",
          "recovery": "预检失败或维护窗口到达时安全停机，保留未完成作业和材料账；替换件不足就停在可恢复状态，拆下件送 `metallurgy.recycling`，回收金属经再分选、再熔炼和再认证后才能回装。"
        }
      ],
      "familyLinks": [
        {
          "from": "geology.strata-survey",
          "to": "geology.ore-prospecting",
          "kind": "supports",
          "reason": "汇总的层理、空洞与置信度证据限定下一步探测范围；没有可复核测绘就不能把探测结果当成开采许可。"
        },
        {
          "from": "geology.ore-prospecting",
          "to": "metallurgy.ore-washing",
          "kind": "supports",
          "reason": "矿脉类别、杂质线索和风险决定使用哪种分选路径；无来源或矛盾读数的批次进入拒收/补测，而不是直接加工。"
        },
        {
          "from": "metallurgy.ore-washing",
          "to": "metallurgy.smelting-control",
          "kind": "supports",
          "reason": "分级净料与杂质记录决定熔炼控温的输入配置，混批必须停机，副产物仍在同一材料账中。"
        },
        {
          "from": "metallurgy.smelting-control",
          "to": "mechanics.drive-shaft",
          "kind": "supports",
          "reason": "只有带热历史和性质证书的金属坯才能成为动力轴零件，原矿和未认证坯在装配入口被拒绝。"
        },
        {
          "from": "mechanics.maintenance",
          "to": "metallurgy.recycling",
          "kind": "supports",
          "reason": "分析器生成部件级维修计划，并把拆下的失效件按来源交给金属回收；维修计划和材料账必须原子一致。"
        },
        {
          "from": "metallurgy.recycling",
          "to": "metallurgy.ore-washing",
          "kind": "supports",
          "reason": "回收后的金属仍可能混有镀层与杂质，必须重新进入分选并再次取得可熔炼来源，防止回收成为绕过属性门禁的捷径。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "geology",
          "familyId": "strata-survey",
          "familyKey": "geology.strata-survey",
          "reason": "把地下结构从不可见风险变成可共享证据，确立“先读岩、再动镐”的第一道闸门；它为 `geology.ore-prospecting` 提供非破坏输入，直接阻断盲挖直达终局材料。",
          "stories": [
            {
              "itemId": "geology.strata-survey.probe",
              "order": 1,
              "text": "复原者先在露头取得不破坏样本，读出层理方向与空洞警示；代价是采样时间和探针耐久，证据不足时只能停在“待补采”，不会因为挥镐就掉出深层材料。"
            },
            {
              "itemId": "geology.strata-survey.analyzer",
              "order": 2,
              "text": "把多点样本解析成可比较的地层剖面，明确“支持继续勘探”或“读数冲突”；冲突样本与失败原因必须保留，并退回 `geology.strata-survey.probe` 补测，不能把猜测伪装成矿脉结论。"
            },
            {
              "itemId": "geology.strata-survey.station",
              "order": 3,
              "text": "观测站把坐标、层理、风险和置信度固化为可复核证据包，才允许 `geology.ore-prospecting` 消费这份证据；建立固定观测点和维护记录是成本，输出仍是证据而不是终局材料。"
            }
          ]
        },
        {
          "disciplineId": "geology",
          "familyId": "ore-prospecting",
          "familyKey": "geology.ore-prospecting",
          "reason": "把测绘证据转成“是否、哪里、以何种风险开采”的决定，让玩家理解深层材料必须先付出勘探成本；高阶产出是带来源的原矿批次与批准区段，而非一步掉落的终局材料。",
          "stories": [
            {
              "itemId": "geology.ore-prospecting.probe",
              "order": 1,
              "text": "沿已登记的地层证据做有界方向与距离探测，得到候选矿脉而不是成品；没有有效测绘记录就返回“未知”，玩家支付时间和工具磨损后仍须回到 `geology.strata-survey`。"
            },
            {
              "itemId": "geology.ore-prospecting.analyzer",
              "order": 2,
              "text": "比较多次读数，判断矿脉连续性、品位线索和断层风险；样本互相矛盾时只生成补测任务并保留原读数，失败教训是“更多读数不等于更高品位”，不能跳过分选。"
            },
            {
              "itemId": "geology.ore-prospecting.station",
              "order": 3,
              "text": "把确认后的区段、开采路径和原矿来源绑定成批准计划，采出的原矿批次带着来源与风险标签交给 `metallurgy.ore-washing`；计划外开采只能进入拒收/风险处理，不会凭空生成可冶炼终局材料。"
            }
          ]
        },
        {
          "disciplineId": "metallurgy",
          "familyId": "ore-washing",
          "familyKey": "metallurgy.ore-washing",
          "reason": "原矿不是金属，先把杂质和等级分开才能让后续性质可解释；这条家族把 `geology.ore-prospecting` 的证据落实为可审计的分选产物，并在加热前建立材料守恒。",
          "stories": [
            {
              "itemId": "metallurgy.ore-washing.reagent",
              "order": 1,
              "text": "对已批准的原矿批次执行第一次分离，显式产出净料与杂质/石屑副产物；水、试剂和处理时间是真实代价，无来源或混批输入留在拒收槽，不能被偷偷吞掉。"
            },
            {
              "itemId": "metallurgy.ore-washing.core",
              "order": 2,
              "text": "按勘探记录中的矿石类别与杂质线索选择分级路径，把不同等级净料送往对应的 `metallurgy.smelting-control`，混入不匹配批次就停机并退回；副产物保持可计数、可返洗，失败不会变成免费增产。"
            },
            {
              "itemId": "metallurgy.ore-washing.vat",
              "order": 3,
              "text": "关闭一批原矿的守恒账：来源输入必须对应净料、残渣、返洗料、拒收料或未处理库存，账不闭合就保留批次并要求复核；只有闭合的净料批次才能进入控温熔炼。"
            }
          ]
        },
        {
          "disciplineId": "metallurgy",
          "familyId": "smelting-control",
          "familyKey": "metallurgy.smelting-control",
          "reason": "同一种矿物经过不同成分与热史并不等于同一种材料；控温把净料变成有性质证书的金属坯，并把炉渣和返工出口留下，作为机械装配前不可绕过的第二道闸门。",
          "stories": [
            {
              "itemId": "metallurgy.smelting-control.reagent",
              "order": 1,
              "text": "给分选净料设定目标温度窗口与冷却条件，燃料、热量和等待时间成为性质控制的代价；偏离窗口时安全停炉并保留原批次，不把失败重抽成另一种材料。"
            },
            {
              "itemId": "metallurgy.smelting-control.core",
              "order": 2,
              "text": "控制芯按用途选择强度、耐热或导电等性质目标，并要求同质净料；输入混杂或配方不符时输出“未认证坯”并说明原因，禁止把普通锭伪装成合格结构件。"
            },
            {
              "itemId": "metallurgy.smelting-control.vat",
              "order": 3,
              "text": "反应釜记录熔炼、保温、冷却的热历史，分别产出带性质证书的金属坯和可回收炉渣/返工料；只有证书完整的批次才能被 `mechanics.drive-shaft` 接收，冷却失败沿回收路径重熔而非跳过工艺。"
            }
          ]
        },
        {
          "disciplineId": "mechanics",
          "familyId": "drive-shaft",
          "familyKey": "mechanics.drive-shaft",
          "reason": "将冶金的性质证书转成受边界约束的传动能力，强调“材料性能必须在结构中兑现”；这个家族让玩家看到装配前检查、运行中磨损和机械能力之间的因果。",
          "stories": [
            {
              "itemId": "mechanics.drive-shaft.part",
              "order": 1,
              "text": "用带性质证书的金属坯装配动力轴零件，先做材质兼容与几何预检；原矿、未分选料或未认证坯直接退回返工，检查失败不扣除尚未开始的材料。"
            },
            {
              "itemId": "mechanics.drive-shaft.drive",
              "order": 2,
              "text": "将通过预检的零件组合成有长度、负载和磨损边界的动力轴机芯，同时写入材料来源；过载或连接错误时安全停机并保留工单，不能以无限动力掩盖材料等级不足。"
            },
            {
              "itemId": "mechanics.drive-shaft.workstation",
              "order": 3,
              "text": "把动力轴机芯装入作业台，公开输入、输出、负载、磨损和维护槽位，才形成可持续的机械能力；机器的产能来自合格材料与持续维护，不是从一个终局材料直接复制产物。"
            }
          ]
        },
        {
          "disciplineId": "mechanics",
          "familyId": "maintenance",
          "familyKey": "mechanics.maintenance",
          "reason": "把“坏了就重摆”改为可观察、可恢复的维护循环：预警先停机，计划锁定材料，拆件回收，再认证回装；它把旧文明无界自动化的失败教训落实为日常玩法。",
          "stories": [
            {
              "itemId": "mechanics.maintenance.probe",
              "order": 1,
              "text": "检查动力轴与作业台的磨损、污染、来源和最近一次失败，在接近维护窗口时停止接收新任务；检查耗时换来可预期停机，避免旧文明式的无声断裂。"
            },
            {
              "itemId": "mechanics.maintenance.analyzer",
              "order": 2,
              "text": "把检查结果拆成清理、更换、重铸和回收步骤，预留精确材料并把拆下件送入 `metallurgy.recycling`；库存或性质不匹配时整张维修计划保持未结算，不产生半修半坏状态。"
            },
            {
              "itemId": "mechanics.maintenance.station",
              "order": 3,
              "text": "在维护窗口执行替换/复检，恢复未完成工单并留下维修前后状态；拆下件经 `metallurgy.recycling` 回收后必须重新走分选与控温认证，缺件时机器保持安全停机而不是丢失材料或整机重造。"
            }
          ]
        }
      ]
    },
    {
      "id": "W6",
      "order": 6,
      "title": "W6 野外科学：把代价带回实验台",
      "state": "implemented",
      "disciplineIds": [
        "chemistry",
        "environment",
        "exploration"
      ],
      "purpose": "把分析、远征、有限采样、化学处理、环境修复和再次出发串成可解释的守恒闭环：反应有输入与状态，废液和残渣有去处，远征只带回有来源且有额度的样本。",
      "motivation": "玩家想用遗迹样本突破材料瓶颈并为 W7 找到可靠媒介，但检测耗材、携带空间、返程时间、反应能量、废液容量和修复工时都必须入账。",
      "crisis": "未知液体、粉尘和遗迹样本会把材料瓶颈变成处理事故：没有基线分析，反应不可读；没有捕获，废液无处去；没有空气复测，影响会累积。玩家必须沿分析、远征、返程、处理、修复顺序找出缺口。",
      "continuityIn": "承接 W5 的勘探结果、材料批次与可维护工位；本波先以 `chemistry.water-analysis` 对本地工位/储液样本建立基线，再以这份记录放行有限远征。",
      "continuityOut": "完成返程样本复测、化学处理、废液与残渣回收、空气影响复测并封存远征记录后，向 W7 交出带 sourceId 的有限样本、影响状态与回程证据。",
      "gate": "先完成本地基线分析→有限远征→返程样本封存与复测→酸碱处理→废液/残渣回收→环境修复与复测→下一次放行；任一步失败都保留批次和停点。",
      "verificationGate": "运行行为门禁：先以一个既有带来源样本形成 `chemistry.water-analysis` 基线准入，再完成有安全节点与返程记录的有限远征；重复扫描同一地点必须返回同一领取状态而不新增样本。任一许可内样本批次进入 acid-base 处理时，错配可锁定恢复；全部废液、回用水、残渣和空气影响有明确出口与复测。修复污染后同一 expeditionId 才可再次开放，判定不依赖样本数量。",
      "disciplineArcs": [
        {
          "id": "chemistry",
          "role": "把田野样本变成可观察、可追责的反应输入，承担检测与处理代价而不是把未知物当免费原料。",
          "whyNow": "W5 让材料可开采和成型，却会把成分不明的液体、粉尘带入工坊；在 W7 的魔法/空间/引力跨域前，必须先建立化学状态和停机证据。",
          "input": "W5 带来源标签的矿物/粉尘批次、远征水样和标记酸液；主要锚点沿用 chemistry.water-analysis 的玻璃瓶、书、红石粉，以及 chemistry.acid-base 的玻璃瓶、石灰、水桶输入。",
          "output": "可读的液体种类/比例与反应前后记录、被许可的中和液，以及明确标记为待诊断或待净化的批次；不凭空生成样本或吞掉未知输入。",
          "recovery": "探针/解析台/观测站读数缺失或摇摆时保留最后有效记录；反应釜遇到错配就锁定批次，通过补容器、修管路、重新分析后从停点续作，实验室安全记录解释原因。"
        },
        {
          "id": "environment",
          "role": "把生产外部性变成可监测、可回收、可复测的工作量，让修复成为产能恢复的必要步骤。",
          "whyNow": "化学反应的副产物和 W5 工位的烟尘会把代价推给河流、土地与聚落；若不在此波建立回收门槛，后续跨域能力只会放大污染和事故。",
          "input": "化学处理留下的带标签废液、固体残渣和燃烧/粉尘活动；主要锚点使用 environment.water-cleanup 的木炭、玻璃、水桶，以及 environment.air-quality 的玻璃、红石粉、铁锭监测器材。",
          "output": "局部空气影响与来源记录、经净化的可回用水、被保留并转交下游的固体残渣，以及可再开工/继续修复的明确状态。",
          "recovery": "先用 air-quality 定位并隔离源头，再让 water-cleanup 保留待处理液体；滤材、泵、出口或容量故障时停上游不丢批次，修复后继续，最后复测并写入 environment.impact-report。"
        },
        {
          "id": "exploration",
          "role": "把未知区域变成有证据、有容量、有回程的采样行程，承担携带、时间和失败返航成本。",
          "whyNow": "反应链需要真实来源样本，但旧文明的崩解证明“远征=无限资源井”会重演抽取失控；先建立地图、扫描、封存和回程，W7 才能安全接收 W6 的成果。",
          "input": "有补给与回程条件的路线清单、地图/纸/指南针，以及望远镜、青金石、红石粉支持的遗迹扫描；每次远征还要带有限样本容纳与封存能力。",
          "output": "已走通的安全节点、当前维度与回程证据、遗迹轮廓和一次性/有限额度的带来源样本包；重复访问返回既有状态，不制造无限掉落。",
          "recovery": "路线、坐标或维度失配时回到最后安全节点；扫描签名不符就隔离样本，营地保留清单与包裹，回到 chemistry.water-analysis 或 environment 修复后再继续，不重置地点状态。"
        }
      ],
      "familyLinks": [
        {
          "from": "chemistry.water-analysis",
          "to": "exploration.survey-map",
          "kind": "supports",
          "reason": "分析结果建立维度、液体状态和可接受风险的出发条件，只有读懂样本才值得规划远征。"
        },
        {
          "from": "exploration.survey-map",
          "to": "exploration.ruin-scan",
          "kind": "supports",
          "reason": "安全节点与当前维度记录限定遗迹扫描路线，未经确认的地形不产生采样许可。"
        },
        {
          "from": "exploration.ruin-scan",
          "to": "exploration.field-camp",
          "kind": "supports",
          "reason": "扫描只产生带来源的有限样本记录，样本必须在野外营地封存、携回，不能直接变成无限掉落。"
        },
        {
          "from": "exploration.field-camp",
          "to": "chemistry.water-analysis",
          "kind": "supports",
          "reason": "营地的临时补给与封存状态为返回样本提供可恢复的中继点，再交给实验室分析。"
        },
        {
          "from": "chemistry.water-analysis",
          "to": "chemistry.acid-base",
          "kind": "supports",
          "reason": "水质、比例和来源标签是酸碱处理的准入证；未知或混合输入先诊断，不得盲反应。"
        },
        {
          "from": "chemistry.acid-base",
          "to": "chemistry.lab-safety",
          "kind": "supports",
          "reason": "容器、试剂或状态失配时交给实验室安全边界锁定批次，保留输入并提供恢复原因。"
        },
        {
          "from": "chemistry.acid-base",
          "to": "environment.water-cleanup",
          "kind": "supports",
          "reason": "中和过程产生的待处理液体必须进入收集槽，反应完成不等于外部性消失。"
        },
        {
          "from": "environment.water-cleanup",
          "to": "environment.waste-processing",
          "kind": "supports",
          "reason": "净化分出的固体残渣有明确去处；出口未就绪时保持隔离和待处理状态。"
        },
        {
          "from": "environment.water-cleanup",
          "to": "environment.air-quality",
          "kind": "supports",
          "reason": "净化后还要检查是否把污染转移成新的排放，水体通过不代表空气影响已清零。"
        },
        {
          "from": "environment.air-quality",
          "to": "exploration.survey-map",
          "kind": "supports",
          "reason": "空气源头被隔离且影响记录复测通过，才向地图开放下一次出发；否则从污染停点恢复。"
        },
        {
          "from": "environment.air-quality",
          "to": "environment.impact-report",
          "kind": "supports",
          "reason": "监测趋势与修复前后记录汇总为长期影响账本，防止一次短时读数覆盖累积外部性。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "chemistry",
          "familyId": "water-analysis",
          "familyKey": "chemistry.water-analysis",
          "reason": "把“看不见的外部性”变成进入任何反应前必须读懂的信号，承担检测耗材、时间和等待代价，连接远征样本与可审计处理。",
          "stories": [
            {
              "itemId": "chemistry.water-analysis.probe",
              "order": 1,
              "text": "复原者先读取 W5 留下的本地工位或储液中的带 sourceId 样本，建立颜色、状态和比例基线；之后带回的远征样本只能与这份基线复测，未知样本保持隔离，不能直接喂给反应链。"
            },
            {
              "itemId": "chemistry.water-analysis.analyzer",
              "order": 2,
              "text": "把解析模组装入解析台，连续读数对照来源混杂与浓度变化，并留下反应前后的两份记录；断管或混液时报告缺口和来源，保留样本，修复连接或冲洗后再测，不把失败伪装成净化。"
            },
            {
              "itemId": "chemistry.water-analysis.station",
              "order": 3,
              "text": "接入能量的观测站把分析记录、来源标签与复测结果合成为反应许可；读数摇摆就保持最后有效记录并进入待诊断，隔离污染源并重测后才能从原批次继续。"
            }
          ]
        },
        {
          "disciplineId": "chemistry",
          "familyId": "acid-base",
          "familyKey": "chemistry.acid-base",
          "reason": "让化学不再是黑箱配方：玩家为可见的配比、容器、能量与停机边界付费，并把错误转化为可诊断、可恢复的教训。",
          "stories": [
            {
              "itemId": "chemistry.acid-base.reagent",
              "order": 1,
              "text": "只接受已标记的酸液和匹配试剂，中和前后用状态变化与记录显示反应；试剂和容器容量就是成本，标签不匹配时拒绝消耗并指出所需输入。"
            },
            {
              "itemId": "chemistry.acid-base.core",
              "order": 2,
              "text": "反应芯把每批中和的输入、输出和试剂消耗绑定在一起，容器缺失、来源不明或输入错误时锁定批次；修正容器或标签后从保留批次恢复，而不是丢液或重抽样。"
            },
            {
              "itemId": "chemistry.acid-base.vat",
              "order": 3,
              "text": "加热反应釜将待处理、反应中、已中和三态公开给玩家，并把结果交给净化链；它提高可观测吞吐而不复制产物，前后读数不一致就停热、退回水质分析，复测通过后再续作。"
            }
          ]
        },
        {
          "disciplineId": "environment",
          "familyId": "air-quality",
          "familyKey": "environment.air-quality",
          "reason": "把生产的空气代价放到玩家眼前，并把“停止源头—处理影响—复测放行”设成可解释的再开工门槛。",
          "stories": [
            {
              "itemId": "environment.air-quality.probe",
              "order": 1,
              "text": "在燃烧或粉尘工位旁读取空气状态，灰色粒子首次把产量背后的烟尘外部性展示出来；监测范围有限，玩家必须把探针带到源头，而不是获得全世界的免费全知。"
            },
            {
              "itemId": "environment.air-quality.analyzer",
              "order": 2,
              "text": "解析模组连续对照作业活动与局部空气趋势，帮助区分短暂波动和仍在排放的源头；超过承载线时给出来源诊断并暂停放行，不能用一次漂亮读数抹平累积影响。"
            },
            {
              "itemId": "environment.air-quality.station",
              "order": 3,
              "text": "观测站在净化前后签出可读的影响记录，未隔离燃烧源或粉尘源就不给“可再出发”状态；误报或断能时保留最后有效记录，修复源头、补测后恢复，而非永久封锁区域。"
            }
          ]
        },
        {
          "disciplineId": "environment",
          "familyId": "water-cleanup",
          "familyKey": "environment.water-cleanup",
          "reason": "让污染物拥有去处和回收责任：净化不是删除外部性，而是把水、残渣、容量与下游处理一起纳入守恒闭环。",
          "stories": [
            {
              "itemId": "environment.water-cleanup.filter",
              "order": 1,
              "text": "把化学链送来的带标签废液先经过滤芯，净水与固体残渣分开可见；滤材和残渣容纳量是必须准备的代价，输入异常或容器已满时留在收集槽，不准直接排掉。"
            },
            {
              "itemId": "environment.water-cleanup.pump",
              "order": 2,
              "text": "泵只推动已捕获的废液进入受限处理路径，液位、堵塞和待处理批次都可观察；管路满载就停上游并保留批次，清空残渣出口或修复连接后从停点恢复，失败不会变成无来源的损失。"
            },
            {
              "itemId": "environment.water-cleanup.network",
              "order": 3,
              "text": "完整管网同时管理废液、回收水和固体残渣，残渣必须转交 environment.waste-processing，且要等空气复测通过才算修复完成；任一出口失效即隔离支路，修好后继续未结批次，不以丢弃废料换取绿灯。"
            }
          ]
        },
        {
          "disciplineId": "exploration",
          "familyId": "survey-map",
          "familyKey": "exploration.survey-map",
          "reason": "把远征从无限刷点改成有准备、有证据、有返程的行程，地图进度成为处理许可与下一次出发的前置条件。",
          "stories": [
            {
              "itemId": "exploration.survey-map.probe",
              "order": 1,
              "text": "沿准备好的路线写入一个已走通的安全节点，地图、纸张、指南针和行程时间构成远征成本；没有证据的地形只标成未知，不因地图工具凭空产生资源。"
            },
            {
              "itemId": "exploration.survey-map.analyzer",
              "order": 2,
              "text": "解析模组把已访问节点、当前维度和样本申领状态合成路线清单；同一地点再次读取只返回已有状态，不刷新掉落，坐标或维度不符时退回最后安全节点并保留清单。"
            },
            {
              "itemId": "exploration.survey-map.station",
              "order": 3,
              "text": "通电观测站在营地封存地图、样本数和回程确认，未封存、退回或隔离当前批次就不开下一条路线；断能或迷路后以最后站点记录恢复，不重复申领地点。"
            }
          ]
        },
        {
          "disciplineId": "exploration",
          "familyId": "ruin-scan",
          "familyKey": "exploration.ruin-scan",
          "reason": "明确“发现”与“获得”的边界：遗迹只给有限、带来源的样本，玩家必须承担辨识、携带、回程和失败后的复核成本。",
          "stories": [
            {
              "itemId": "exploration.ruin-scan.probe",
              "order": 1,
              "text": "先扫描遗迹轮廓再决定取样点，白色边缘告诉玩家哪里可以安全观察；扫描本身不破坏方块、不自动掉落，玩家要用携带空间和回程时间承担一次有限采样。"
            },
            {
              "itemId": "exploration.ruin-scan.analyzer",
              "order": 2,
              "text": "解析模组核对遗迹来源、坐标与样本签名，并把一次已领取样本写入地点记录；重复扫描返回已领取或已耗尽状态，签名不符则隔离样本并按地图回撤，不靠重刷来掩盖失败。"
            },
            {
              "itemId": "exploration.ruin-scan.station",
              "order": 3,
              "text": "观测站把样本、路线和采集者信息封存成可交给 chemistry.water-analysis 的包裹；包裹不完整或污染时保留记录、退回复测或送环境修复，遗迹不会因失败重新生成无限样本。"
            }
          ]
        }
      ]
    },
    {
      "id": "W7",
      "order": 7,
      "title": "W7 · 灵魂共振：有主的意图、可返航的路径",
      "state": "implemented",
      "disciplineIds": [
        "magic",
        "space",
        "gravity"
      ],
      "purpose": "把灵魂共振收敛为可审计的跨域责任链：魔法签发有主、限时、可撤销的意图，空间提供版本化路径与返航端点，引力按载荷形成有限且可复核的运输成本。",
      "motivation": "复原者要让远端遗址重新接入城邦：带去有限的修复物资、拿回遗址记录，并确保每一件货物和每一次魔能消耗都能回到责任人名下。玩家的收益不是一条无代价捷径，而是第一次拥有可信的远征路线；准备不足会明确暴露在意图、返航、容量或成本中的缺口，修复后还能从恢复仓取回未确认货物。",
      "crisis": "残留共振接受无主意图、过期路线或超重货物；任何一项证明缺失都可能让修复物资与责任人失联。玩家必须以同一 transitId 绑定意图、路径、载荷和成本，并为过境失败保留回源或恢复仓。",
      "continuityIn": "消费 W6 交出的有限样本、sourceId、影响复测与返程记录；W7 只处理已知端点之间有主、有预算、可恢复的货运，不把“独立信号”或魔法权限当成免费成本。",
      "continuityOut": "输出带 transitId、owner、用途、source/target、routeVersion、payloadDigest、loadUnits、typed cost ledger、阶段状态和 recoveryKey 的运输记录，供 W8 作为已知端点交接证据。",
      "gate": "运行 Gate：一次真实验收必须由同一 transitId 关联 intentReceipt、routeReceipt、loadReceipt；成本按“路线基础/距离与维度因子 + loadUnits 载荷因子 + 完整返航预留”确定性计算，magicSpent、spaceSpent、gravityReserved 及每次释放都写入账本。只有三证齐全、目标安全、平台未超载、源点和返航端点仍可用时，才允许 prepare→open→arrive→confirm。必须分别注入无主/过期意图、路线过期或落点不安全、超载/余额不足、断能/结构损坏四类故障：前三类不得开启且不得产生未授权扣款，过境故障必须回源或进入按 source anchor 索引的 recovery escrow；重复请求、载荷变更、重试和回收后重放均不得复制货物或能量。仅在这一闭环通过后，才可把章节交给 W8；数值平衡只在真实生存数据证明瓶颈后调整，运行注册与配方以当前 manifest 为准。",
      "verificationGate": "运行行为门禁：同一 transitId 必须关联有 owner/nonce/payloadDigest 的 intentReceipt、带 routeVersion/双锚/返航点的 routeReceipt、带 loadUnits/容量的 loadReceipt，以及由有限账户实际支持的 typed cost ledger；电力成本使用 milli-SE，预留、已花费、损耗和释放可对账。未授权/过期、落点失效、超载/余额不足、在途断能分别 fail closed；在途失败只能回源或进入单一 recovery escrow。重复请求、改载荷、重试与恢复均不得复制货物或跳过成本。",
      "disciplineArcs": [
        {
          "id": "magic",
          "role": "意图与绑定 authority：把灵魂共振落成可归属、可限时、可撤销的责任票据。",
          "whyNow": "法杖、ward-circle 与 soul-binding 已形成可执行授权链；W7 用 owner、nonce、有效期和撤销状态证明跨域请求有明确责任人，而不是只证明魔能有载体。",
          "input": "运行输入为 owner UUID、用途代码、目标 anchor ID、payloadDigest、有效期、nonce 与可用魔能预留；不接受只有一句愿望的匿名请求。",
          "output": "输出 intentReceipt：明确责任主体、用途、目标、有效期、撤销状态和载荷摘要；魔法只证明“谁有权发起什么”，不替空间选择路线，也不替引力决定成本。",
          "recovery": "owner 不存在、权限不符、票据过期、被撤销或载荷摘要变化时，拒绝开放且不消耗未结算资源；已签发但未启程的意图可撤销，途中故障则冻结 receipt 并转入 recovery，不静默换目标。"
        },
        {
          "id": "space",
          "role": "路径与返航 authority：把坐标移动变成版本化、可验证、可回退的路线。",
          "whyNow": "space-dust、local-anchor 与 rift-safety 已形成双锚和返航链；W7 必须冻结 routeVersion、落点安全与恢复路径，不能把可达误写成可安全交付。",
          "input": "运行输入为有效 intentReceipt、源/目标维度与坐标、source anchor、target anchor、routeVersion、返航端点及目标安全证明。",
          "output": "输出 routeReceipt：冻结路线版本、距离/维度因子、目标安全结果、返航路径、阶段状态和 recoveryKey；空间负责“怎么走以及怎么回来”，不隐藏货物质量。",
          "recovery": "源点或目标不安全、路线过期、门框/路径损坏时在 prepare 阶段拒绝并释放未用预留；过境后确认失败则使用返航预留回源，回源暂不可用时锁入按源锚索引的恢复仓，等待修复与重新验证。"
        },
        {
          "id": "gravity",
          "role": "成本与载荷 authority：把重量、容量、能量预算和停机责任绑定到同一份运输事实。",
          "whyNow": "mass-reading 与 load-platform 已把载荷、容量和能量预算接入同一运输事实；W7 以可核对成本和安全停机证明设施完整，而不是用魔法或空间隐藏重量债务。",
          "input": "运行输入为冻结的 payload manifest、质量等级与 loadUnits、路线因子、平台容量、可用引力储备和返航预留；任何载荷变化都触发重新称量。",
          "output": "输出 loadReceipt：载荷摘要、loadUnits、容量占用、确定性成本分项、预留/已结算金额和停机阈值；引力承担可见的代价与载荷，不能由魔法或空间偷偷兜底。",
          "recovery": "超载、余额不足、断能或平台结构失效时安全停机，保留输入并释放未用预留；已开始但未确认的货物走返航或 recovery escrow，按已完成阶段记账，禁止重复扣款、复制或把重量债务转给目的地。"
        }
      ],
      "familyLinks": [
        {
          "from": "magic.ward-circle",
          "to": "magic.soul-binding",
          "kind": "supports",
          "reason": "先划定授权工位，再允许签发跨域意图；区域边界防止无主广播。"
        },
        {
          "from": "magic.soul-binding",
          "to": "space.local-anchor",
          "kind": "supports",
          "reason": "绑定携带 owner、用途和目标 anchor ID，空间路由不能解释匿名愿望。"
        },
        {
          "from": "space.local-anchor",
          "to": "space.rift-safety",
          "kind": "supports",
          "reason": "配对源点和目标后，必须把路线版本交给裂隙安全检查。"
        },
        {
          "from": "space.rift-safety",
          "to": "gravity.mass-reading",
          "kind": "supports",
          "reason": "目标安全证明与路线风险因子进入质量计量；安全路径不等于免费路径。"
        },
        {
          "from": "gravity.mass-reading",
          "to": "gravity.load-platform",
          "kind": "supports",
          "reason": "质量等级累加为 loadUnits，平台容量与运输成本必须使用同一载荷事实。"
        },
        {
          "from": "gravity.load-platform",
          "to": "space.local-anchor",
          "kind": "supports",
          "reason": "只有容量接受且返航储备仍在，空间门才可继续；超载把系统带入可恢复停机。"
        },
        {
          "from": "space.rift-safety",
          "to": "magic.soul-binding",
          "kind": "supports",
          "reason": "路径失效会让原绑定进入 revoked/recovery，而不是沿旧意图自动改道。"
        },
        {
          "from": "gravity.mass-reading",
          "to": "magic.soul-binding",
          "kind": "supports",
          "reason": "最终 loadReceipt 成为绑定的一部分；载荷改变即意味着原意图不再有效。"
        },
        {
          "from": "magic.ward-circle",
          "to": "gravity.load-platform",
          "kind": "supports",
          "reason": "授权范围覆盖装载工位，未授权货物不能借合法意图混入平台。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "magic",
          "familyId": "ward-circle",
          "familyKey": "magic.ward-circle",
          "reason": "“守护圆环”把灵魂意图限制在有主、有边界的工位，承担跨域请求的授权入口；它是责任规则而非装饰性法术。",
          "stories": [
            {
              "itemId": "magic.ward-circle.rune",
              "order": 1,
              "text": "复原者先在残骸阵室划出带 owner UUID 与用途字段的工作边界；边界外的施法请求直接拒绝，让玩家明白“先声明谁负责、要做什么”，而不是把魔法当成公共广播。"
            },
            {
              "itemId": "magic.ward-circle.wand",
              "order": 2,
              "text": "法器要求操作者确认目标锚点并签发可撤销的意图票；换操作者、换目标或超出有效期都会失效，失败教训是“意图不等于无限权限”。"
            },
            {
              "itemId": "magic.ward-circle.array",
              "order": 3,
              "text": "法阵只在 intentReceipt、routeReceipt、loadReceipt 三证齐全后开放，并把 transitId 与预留成本展示在阵面；魔法负责授权，不得绕过空间路径和引力载荷审查。"
            }
          ]
        },
        {
          "disciplineId": "magic",
          "familyId": "soul-binding",
          "familyKey": "magic.soul-binding",
          "reason": "“灵魂绑定”把 SoulTech 的灵魂概念落成 UUID 归属、用途、有效期、撤销和载荷不可篡改规则，防止跨域权力无主化。",
          "stories": [
            {
              "itemId": "magic.soul-binding.rune",
              "order": 1,
              "text": "符文把 owner UUID、用途和有效期写入法术载体；非所有者使用时拒绝执行，玩家必须为每次能力指定责任主体，不能把失控归咎于“魔力自己做的”。"
            },
            {
              "itemId": "magic.soul-binding.wand",
              "order": 2,
              "text": "法器封存目标锚点与 payload manifest；任何物品增删或目标改写都会使绑定失效，必须重新称量、重新签发，防止把超载货物藏进旧意图。"
            },
            {
              "itemId": "magic.soul-binding.array",
              "order": 3,
              "text": "法阵将 owner、意图、路线版本、载荷回执和撤销者写成不可静默改写的 transit receipt；启程前可撤销，途中失败则冻结凭据与货物进入恢复态，而不是自动改投另一目的地。"
            }
          ]
        },
        {
          "disciplineId": "space",
          "familyId": "local-anchor",
          "familyKey": "space.local-anchor",
          "reason": "“本地锚定”把空间从瞬移捷径变成有源点、有版本、有返航端点的路线服务，是返航责任的根。",
          "stories": [
            {
              "itemId": "space.local-anchor.shard",
              "order": 1,
              "text": "碎片先记录源坐标与维度，生成唯一 origin anchor；没有可验证的出发点就不能打包移动，玩家因此先建立可回去的家。"
            },
            {
              "itemId": "space.local-anchor.anchor",
              "order": 2,
              "text": "锚将源点与目标点配对，并冻结 routeVersion、维度、坐标和返航端点；目标或路径变更会让票据过期，不能沿着旧地图盲传。"
            },
            {
              "itemId": "space.local-anchor.gate",
              "order": 3,
              "text": "门只有在出发成本和返航成本都已预留、源点仍可回收时才进入 prepare；按 prepare→open→arrive→confirm 状态推进，未满足返航条件不开放。"
            }
          ]
        },
        {
          "disciplineId": "space",
          "familyId": "rift-safety",
          "familyKey": "space.rift-safety",
          "reason": "“裂隙安全”把跨维度移动的失败变成可观测、可回源、可恢复的状态机；它约束空间路径不能把风险转嫁给玩家或货物。",
          "stories": [
            {
              "itemId": "space.rift-safety.shard",
              "order": 1,
              "text": "碎片在扣费前检查目标站立面、世界边界、维度许可和目标可用性；任一证明缺失就拒绝出发且不扣已预留之外的成本，安全检查先于冒险。"
            },
            {
              "itemId": "space.rift-safety.anchor",
              "order": 2,
              "text": "锚同时保存目标安全证明和源点回退证明；准备阶段发现落点变化、结构损坏或路径过期时关闭请求、释放未用预留并要求重新勘验，不进行无限自动重试。"
            },
            {
              "itemId": "space.rift-safety.gate",
              "order": 3,
              "text": "门执行双阶段过境：到达后先确认实体与 payload escrow，再完成结算；确认失败则使用返航预留回源，回源暂不可用时把货物锁在以源锚为键的恢复仓，禁止掉落、复制或静默改道。"
            }
          ]
        },
        {
          "disciplineId": "gravity",
          "familyId": "mass-reading",
          "familyKey": "gravity.mass-reading",
          "reason": "“质量读数”把引力代价从玄学强度转成可复核的载荷单位和扣款依据，是跨域运输的成本计量器。",
          "stories": [
            {
              "itemId": "gravity.mass-reading.mass",
              "order": 1,
              "text": "砝码为每件载荷标出轻、中、重质量等级，并在装载前显示；同一距离不再对不同货物收取不可解释的相同代价，玩家能看见自己带去的负担。"
            },
            {
              "itemId": "gravity.mass-reading.gauntlet",
              "order": 2,
              "text": "手套读取已绑定的 payload manifest，按质量等级累加 loadUnits；未登记、增删或替换物品都会拒绝称量并要求重新绑定，避免通过背包变化逃避成本。"
            },
            {
              "itemId": "gravity.mass-reading.field",
              "order": 3,
              "text": "场发生器把 routeVersion 的路径因子、loadUnits 和返航预留写入 loadReceipt，并在开启前锁定确定性扣款；余额不足只产生可解释的拒绝，不产生半程传送或负数能量。"
            }
          ]
        },
        {
          "disciplineId": "gravity",
          "familyId": "load-platform",
          "familyKey": "gravity.load-platform",
          "reason": "“载荷平台”把引力能力变成公开容量、停机和恢复边界，让每次移动都承担与货物重量相称的责任。",
          "stories": [
            {
              "itemId": "gravity.load-platform.mass",
              "order": 1,
              "text": "砝码在平台上标出容量线和当前占用；越过阈值立即停装并保留槽内物品，教玩家先减载而不是期待系统替自己吞下超载。"
            },
            {
              "itemId": "gravity.load-platform.gauntlet",
              "order": 2,
              "text": "手套把货物与 owner、transitId 和返航端点绑定；平台满载、支撑失效或载荷变化时安全停机，卸货后重新称量即可恢复，失败不销毁输入。"
            },
            {
              "itemId": "gravity.load-platform.field",
              "order": 3,
              "text": "场发生器把平台容量、引力储备和空间门状态联锁；断能、超载或结构破坏时停止新作业，按已完成阶段结算、释放未用储备并将未确认货物送入返航/恢复流程，不能把重量债务推给目的地。"
            }
          ]
        }
      ]
    },
    {
      "id": "W8",
      "order": 8,
      "title": "远界航行：观测、采样与完整回程",
      "state": "implemented",
      "disciplineIds": [
        "astral",
        "ender",
        "dimensional"
      ],
      "purpose": "先用星辉定位并长期观测，再以最小扰动采样陌生生态与材料，最后完成受控往返和污染复盘；远界的价值是可复核路线、生态知识与可持续材料替代。",
      "motivation": "远征的收益是把未知变成可复用路线，把陌生生态变成有来源的知识与材料替代，并将完整回程证据带回城邦；掠夺后弃置会破坏样本和下一次定位。",
      "crisis": "星图会断档，门户对应关系会漂移，陌生生态可能被过度采样，未标记材料还会把污染带回城邦。玩家必须在方向、限额、隔离和相位都不完美时完成可重复的完整往返。",
      "continuityIn": "W7 已证明已知端点之间的有主货运；W8 才面对未知航向、陌生生态、污染隔离与相位漂移，以 `space.local-anchor` 的起点证据和 W7 回程记录开始完整远征。",
      "continuityOut": "把回程确认的星图档案、门户记录、带来源样本、污染状态和未解决异常交给 W9，用于保存、复测和公共使用规则。",
      "gate": "同一远征必须完成：出发前确认星图、本地锚、候选落点包络与完整返航预留；首次安全抵达后确认站立面并建立远界第二锚；随后在限额内采样、核对污染与相位状态，并用双锚返回原点或最近有效锚，最后回写实际到达、样本和异常。后续复航才要求出发前双锚均有效。",
      "verificationGate": "运行行为门禁：同一 expeditionId 以最后有效星图、本地锚、候选落点包络和返航预留出发；首次落地只在安全确认后创建远界第二锚。注入相位漂移、落点不可站立或污染时，人员回退、货物隔离、未用预留只释放一次；成功返航和后续复航必须由双锚、来源与恢复 receipt 闭合证明，而不是以采集数量判定。",
      "disciplineArcs": [
        {
          "id": "astral",
          "role": "定位与长期观测：把远界目标从传闻变成可复核的方向、时间窗与连续记录。",
          "whyNow": "W7 的局部锚点能保证站得住，但没有天空基准就无法判断远界路线是否仍有效；必须先建立星座定位和长期档案，避免盲开裂隙。",
          "input": "space.local-anchor、space.route-record，以及已校准的本地天空观测窗口。",
          "output": "astral.constellation 与 astral.star-map 的可复核方位和长期档案，交给 dimensional.rift-chart 生成远征路线；只把最后有效读数作为导航依据。",
          "recovery": "遮云、观测中断或前后读数冲突时保留最后有效星图，不臆测新方位；回到最近的 space.local-anchor，重新观测后才允许继续。"
        },
        {
          "id": "ender",
          "role": "陌生生态与材料采样：把末影区当作需要许可和限额的活系统，而非矿场。",
          "whyNow": "只有在 astral.constellation 和 astral.star-map 给出可复核落点后，采样队才知道落地范围和返程窗口；先取小样本，验证它能否支持城邦的守恒生产。",
          "input": "来自 astral.star-map 的目标条件、首次安全抵达后由 dimensional.dimensional-anchor 确认的远界站点，以及未开封的采样容器；第二锚未建立前只允许观察和回撤。",
          "output": "ender.chorus 的生态观察和繁育材料、ender.endstone 的来源标记样本；所有样本交由 dimensional.foreign-material 标记，未完成回程不得投入本地生产。",
          "recovery": "发现紫颂扩散、样本活性异常或来源不明时立即停止采样，封存并回退到 dimensional.dimensional-anchor；污染样本只进入隔离记录，不以丢弃或倾倒假装恢复。"
        },
        {
          "id": "dimensional",
          "role": "受控往返：把跨维度移动编排成有起点、目标、货单、失稳回退和回程确认的闭环。",
          "whyNow": "定位和采样若没有可靠回程，远征就是一次性掠夺；现在必须验证通道在携带样本、遇到边界或相位漂移时仍能把人和证据带回。",
          "input": "astral.constellation/astral.star-map 的航向与观测窗口、dimensional.rift-chart 的门户关系、本地出发锚和候选落点包络；首次安全落地后再建立远界第二锚。",
          "output": "一份完整往返记录：出发锚点、到达确认、采样清单、污染状态、相位结果与回程确认；成功后才把数据交给 W9。",
          "recovery": "落点漂移、边界越界或锚点失效时由 dimensional.phase-stabilizer 取消过境并回到最后有效锚点；foreign-material 未通过的货物隔离，禁止自动重开门，按 dimensional.return-protocol 重新确认。"
        }
      ],
      "familyLinks": [
        {
          "from": "space.local-anchor",
          "to": "astral.constellation",
          "kind": "supports",
          "reason": "[准备→定位] 先固定本地起点，再在可观测天空窗口取得方向；没有有效起点的星辉读数不能成为航向。"
        },
        {
          "from": "astral.constellation",
          "to": "astral.star-map",
          "kind": "supports",
          "reason": "[定位→长期观测] 每次定位都写入观测条件，重复读数才能判断航向是否稳定，而不是把一次箭头当成永久事实。"
        },
        {
          "from": "astral.star-map",
          "to": "dimensional.rift-chart",
          "kind": "supports",
          "reason": "[定位→航线准备] 只有连续有效观测才能更新门户关系；星图断档时路线降级为待复核，不允许盲开。"
        },
        {
          "from": "dimensional.rift-chart",
          "to": "dimensional.dimensional-anchor",
          "kind": "supports",
          "reason": "[准备] 目标维度、目标坐标和返程坐标必须形成起/终点双锚；落点不满足安全站立条件就不出发。"
        },
        {
          "from": "dimensional.dimensional-anchor",
          "to": "ender.chorus",
          "kind": "supports",
          "reason": "[采样] 先建立远界安全站点，再围绕站点观察和取样，采样范围服从生态恢复能力而非最大化收集。"
        },
        {
          "from": "ender.chorus",
          "to": "ender.endstone",
          "kind": "supports",
          "reason": "[采样] 先解释紫颂的生长和栖息条件，再选择同点的少量岩材；禁止为了材料把生态基底挖空。"
        },
        {
          "from": "ender.endstone",
          "to": "dimensional.foreign-material",
          "kind": "supports",
          "reason": "[污染恢复] 样本离开远界前写入来源维度、用途和隔离状态；来源不明或反应异常的材料不能混入城邦总库存。"
        },
        {
          "from": "ender.chorus",
          "to": "dimensional.return-protocol",
          "kind": "supports",
          "reason": "[回程] 生态观察未完成或出现扩散迹象时，回程协议以带回记录而非带回更多样本为成功条件。"
        },
        {
          "from": "dimensional.phase-stabilizer",
          "to": "dimensional.return-protocol",
          "kind": "supports",
          "reason": "[失稳恢复] 检测到落点漂移或相位波动就取消过境并回到最后有效起点；必须重新确认，不把自动重试当作抵达。"
        },
        {
          "from": "dimensional.return-protocol",
          "to": "astral.star-map",
          "kind": "supports",
          "reason": "[闭环复盘] 返航确认把实际到达、耗时、异常和货单状态写回星图档案，下一次远征必须用这条证据校准路线。"
        }
      ],
      "anchors": [
        {
          "disciplineId": "astral",
          "familyId": "constellation",
          "familyKey": "astral.constellation",
          "reason": "承担远界定位的第一道证据链：把天空从装饰改成可重复验证的航向，失败时保留最后有效定位而不制造虚假确定性。",
          "stories": [
            {
              "itemId": "astral.constellation.probe",
              "order": 1,
              "text": "第一次出发前以本地锚为原点读取单一星座方向；云层或遮挡导致无读数时记录“不可定位”并留在原地，让玩家学会观测失败比猜方向安全。"
            },
            {
              "itemId": "astral.constellation.analyzer",
              "order": 2,
              "text": "把多次读数与观测时段、天气条件对照，剔除不可复现的漂移方向；代价是等待窗口，收益是远征路线不再依赖一次偶然读数。"
            },
            {
              "itemId": "astral.constellation.station",
              "order": 3,
              "text": "长期观测站建立可复核的航向基线；若当前星位与历史档案冲突，航线降为待复核，必须回到上次有效锚点而不是强行开启裂隙。"
            }
          ]
        },
        {
          "disciplineId": "astral",
          "familyId": "star-map",
          "familyKey": "astral.star-map",
          "reason": "承担长期观测与复盘：远界值得去，不是因为一次稀有战利品，而是因为连续记录能让城邦获得可复用、可纠错的路线知识。",
          "stories": [
            {
              "itemId": "astral.star-map.probe",
              "order": 1,
              "text": "把一次可复现方位写成带观测条件的星图条目，玩家获得第一个远界目标，但必须放弃没有证据支持的捷径。"
            },
            {
              "itemId": "astral.star-map.analyzer",
              "order": 2,
              "text": "将多次观测拼成时间序列，判断航线是否仍有可用窗口；夜空被遮挡或资料断档时标记旧路线过期并暂停采样。"
            },
            {
              "itemId": "astral.star-map.station",
              "order": 3,
              "text": "档案站长期记录起点、方向、实际到达与返航差异；远征因此从一次性发现变成可维护的公共知识，回程数据成为下一次准备的输入。"
            }
          ]
        },
        {
          "disciplineId": "ender",
          "familyId": "chorus",
          "familyKey": "ender.chorus",
          "reason": "把陌生生态作为有边界的活系统来理解：高阶成果是可复种、可隔离的知识和材料，而不是对原生群落的最大化抽取。",
          "stories": [
            {
              "itemId": "ender.chorus.seed",
              "order": 1,
              "text": "在已锚定的小片区取得第一份紫颂生长样本，只保留可复种材料并留下原群落；目标是带回生态能力而不是搬空资源，发现扩散迹象就停止采样。"
            },
            {
              "itemId": "ender.chorus.culture",
              "order": 2,
              "text": "对样本做受控培养对照，记录基质、湿度与扰动对生长的影响；活性异常或来源不明时封存样本并退回锚点，不能把实验废料倾倒回远界。"
            },
            {
              "itemId": "ender.chorus.greenhouse",
              "order": 3,
              "text": "建立远界样本的隔离温室，完成回程和污染检查后才开放少量繁育；生态指标越界时关闭温室并隔离批次，不靠继续采集掩盖失控。"
            }
          ]
        },
        {
          "disciplineId": "ender",
          "familyId": "endstone",
          "familyKey": "ender.endstone",
          "reason": "承担陌生材料的最小采样与替代验证：材料只有在来源可追踪、影响可复盘并能减少本地消耗时才值得带回。",
          "stories": [
            {
              "itemId": "ender.endstone.fragment",
              "order": 1,
              "text": "只取一份带来源的末地岩材碎片，验证它是否能替代城邦的高耗材料；采样点出现结构或生物反应时放弃开采并保留原位。"
            },
            {
              "itemId": "ender.endstone.alloy",
              "order": 2,
              "text": "把岩材与本地材料做小批相容性试验，产出可追溯的替代配方；不兼容就隔离该批次，不能混入公共库存再让污染扩散。"
            },
            {
              "itemId": "ender.endstone.block",
              "order": 3,
              "text": "把验证合格的材料固化为可重复的结构单元，并保留原产地与生态影响记录；高阶进展是以替代方案减少掠夺，不是扩大挖掘面。"
            }
          ]
        },
        {
          "disciplineId": "dimensional",
          "familyId": "dimensional-anchor",
          "familyKey": "dimensional.dimensional-anchor",
          "reason": "承担往返的物理前提：首次未知远征先建立本地锚和候选落点，安全抵达后再确认远界第二锚；返航与后续复航才要求两个可站立、可确认的坐标同时有效。",
          "stories": [
            {
              "itemId": "dimensional.dimensional-anchor.shard",
              "order": 1,
              "text": "在出发侧保存可站立原点、目标条件与样本清单，缺任何一项都不启动；第一次门就教会玩家回程必须写在出发前。"
            },
            {
              "itemId": "dimensional.dimensional-anchor.anchor",
              "order": 2,
              "text": "在远界建立第二安全点并与本地起点配对；遇到环境不适或目标缺失时撤回最近有效点，不能依赖临时生成的门。"
            },
            {
              "itemId": "dimensional.dimensional-anchor.gate",
              "order": 3,
              "text": "门设施只有在双锚、航向和货单一致时才承担往返；高阶不是抵达更远，而是在载人载样本时仍保持可复核的起终点闭环。"
            }
          ]
        },
        {
          "disciplineId": "dimensional",
          "familyId": "return-protocol",
          "familyKey": "dimensional.return-protocol",
          "reason": "把返航从附加动作提升为章节门槛：远征的产出必须连同人员、样本来源、异常与回程证据一起回到城邦。",
          "stories": [
            {
              "itemId": "dimensional.return-protocol.shard",
              "order": 1,
              "text": "出发前展示目标维度、目标坐标与返回坐标，玩家确认后才过境；拒绝无确认的探险，代价是放弃一次冲动捷径。"
            },
            {
              "itemId": "dimensional.return-protocol.anchor",
              "order": 2,
              "text": "回程前核对人员、样本、dimensional.foreign-material 来源标记和污染状态；存在未标记样本时只回人不转货，证据完整优先于战利品。"
            },
            {
              "itemId": "dimensional.return-protocol.gate",
              "order": 3,
              "text": "完成一次真正的出发—到达—观察/采样—回程—档案回写，并用 dimensional.phase-stabilizer 处理漂移；未闭环的“到达”不计入门禁，即使带回稀有物也算失败。"
            }
          ]
        }
      ]
    },
    {
      "id": "W9",
      "order": 9,
      "title": "守恒城邦·复原协议",
      "state": "implemented",
      "disciplineIds": [
        "quantum",
        "chronology",
        "civic"
      ],
      "purpose": "让量子高密度状态、时间周期与多人制度组装成可持续城邦：量子层不创造物资，时间层不修改世界时间，城邦层不取消所有权；每次运行都消费已有材料、能量、物流和维护能力。",
      "motivation": "玩家不再追逐一件能替代所有旧设备的神器，而是要让一座真实有人居住的城邦在资源有限时继续亮灯、供水、生产、运输和救援。完成量子交接、周期运行和公共制度后，玩家能亲眼看到一次故障被吸收、一次周期被闭合，并理解每个早期学科为何仍值得维护。",
      "crisis": "城邦重启时同时出现高密度状态饱和、周期任务重叠、公共库存归属不明和能源支路过载；只提高吞吐会复刻故障，玩家必须接受限容、延期、隔离、对账和回收。",
      "continuityIn": "承接前四波的库存、物流、电网与公共记录，并消费 W5 `metallurgy.smelting-control` 的热史/性质证书与 `mechanics.maintenance` 的维修记录，W6 `exploration.ruin-scan` 的样本档案，W7 `space.rift-safety` 的运输记录，以及 W8 `astral.star-map` 与 `dimensional.return-protocol` 的回程档案。",
      "continuityOut": "城邦获得可重跑的服务周期、最后有效状态、公共库存归属和可撤销角色制度；前八波的采集、农业、水利、能源、加工、探索与跨域设施仍在周期中被调用。",
      "gate": "以有限能源、公共/个人库存归属、最小角色权限和最后有效快照启动一项公共服务；注入单节点故障，冻结单一 escrow、保留 remainingWork、告警并保护关键负载；修复后只重放未应用步骤并完成同一周期。",
      "verificationGate": "运行行为门禁：以最小角色权限、公共/个人库存归属、有限能源预算和最后有效快照启动一项持续公共服务；周期中注入单节点故障，量子层冻结单一 escrow，时间层保存 remainingWork 且不修改世界时间，应急与电网保护保住关键负载。修复并跨一次重启后，只重放未应用步骤，完成同一周期；验证每项输入/能量只扣一次、输出只 credit 一次、未提交输入可回收、无量子副本、无时间快进或成本跳过。",
      "disciplineArcs": [
        {
          "id": "quantum",
          "role": "把分散在机器、区块和跨域设施中的库存与运行状态压成可校验、可回滚的高密度状态层，同时把容量和稳定性作为硬边界。",
          "whyNow": "前八波已经形成足够复杂的生产网络，若没有高密度但有限的交接层，远程协作会重复扣料、丢失归属或诱发无界抽取；终局必须把“更快”改写成“更可证明”。",
          "input": "接收 `basic.sieving` 与 `materials.reinforced-thread` 可回收的材料、`technology.energy-storage` 的有限储备、前八波作业快照和 `logistics.inventory-ledger` 的归属记录；输入必须有来源、版本和目标。",
          "output": "产生有限槽位的状态快照、授权端点间的传输收据和可解包的物资交接；成功输出仍保留原所有权，不生成隐藏资源。",
          "recovery": "退相干或端点拒绝时冻结新写入，恢复最后有效快照，退回未提交输入到来源或 `civic.public-inventory`，用 `chronology.temporal-anchor` 去重并由 `chronology.history-archive` 留下失败记录。"
        },
        {
          "id": "chronology",
          "role": "管理周期、截止和历史，把机器、作物、天气、能源与量子交接放进同一套可观察时序；它安排世界中的任务，但不修改服务器世界时间。",
          "whyNow": "量子端点和公共设施都需要背压、窗口与幂等恢复；没有时间学，自动化只会在重载或拥堵时重新消费同一批前八波成果。",
          "input": "接收 `botany.resin-harvesting`、`agroecology.compost-loop`、`hydrology.reservoir`、`energy.biomass` 的周期性输入，以及 `automation.recovery`、量子传输和公共服务的检查点。",
          "output": "产生不重叠的服务周期、阶段检查点、截止状态和可查询历史；周期结算同时标记消耗、返还、延期和下一轮可用副产物。",
          "recovery": "区块卸载、服务中断或时间异常时从 `chronology.task-buffer` 读取剩余状态，凭 `chronology.temporal-anchor` 拒绝重复任务，并用 `chronology.history-archive` 与 `logistics.inventory-ledger` 对账后继续。"
        },
        {
          "id": "civic",
          "role": "把有限物资、能源和跨域能力编排为多人可长期维护的公共制度，让权限、所有权、工程贡献和应急责任都能被审计与撤销。",
          "whyNow": "终局瓶颈从“能不能造出机器”转为“多人能否共同维护机器”；如果没有角色、公共储备和应急回收，高密度技术只会放大争抢与失控。",
          "input": "接收 `construction.settlement-core` 的空间边界、`logistics.inventory-ledger` 的物资谱系、`commerce.public-works` 的公共投入、`defense.alarm-grid` 的事件、`energy.grid-protection` 的隔离状态，以及量子和时间层的服务请求。",
          "output": "产生最小权限角色、公共库存账本、公开工程进度、能源优先级和灾害响应流程；公共设施的消耗、维护与归还对所有相关角色可解释。",
          "recovery": "故障时由 `civic.disaster-response` 告警，撤销失效角色并隔离故障节点；按 `civic.public-inventory`、`logistics.inventory-ledger` 和 `chronology.history-archive` 对账，确认未完成义务后恢复关键服务。"
        }
      ],
      "familyLinks": [
        {
          "from": "basic.sieving",
          "to": "quantum.quantum-storage",
          "kind": "supports",
          "reason": "W1 的筛分产物成为量子快照可承载的物资来源；压缩减少搬运而不增加资源，读取后的残料回到筛分或回收链。"
        },
        {
          "from": "materials.reinforced-thread",
          "to": "quantum.decoherence",
          "kind": "supports",
          "reason": "W1 的韧性材料承担退相干容纳和维修；容器失败时回收可用纤维，不能把维护成本隐藏掉。"
        },
        {
          "from": "technology.energy-storage",
          "to": "quantum.decoherence",
          "kind": "supports",
          "reason": "W1 的储能是量子稳定预算；低储备会触发冻结和延期，量子工程不能绕过基础电网。"
        },
        {
          "from": "construction.settlement-core",
          "to": "civic.role-protocol",
          "kind": "supports",
          "reason": "W3 的聚落边界限制角色协议的作用域，使公共权限同时有空间边界和身份边界。"
        },
        {
          "from": "energy.grid-protection",
          "to": "civic.energy-charter",
          "kind": "supports",
          "reason": "W3 的支路隔离是能源章程的执行层；故障时先切断局部，再保留公共核心，而不是全城停摆。"
        },
        {
          "from": "automation.recovery",
          "to": "chronology.history-archive",
          "kind": "supports",
          "reason": "W4 的未完成任务检查点写入时间档案，恢复只重放未应用步骤，避免区块重载造成重复扣料。"
        },
        {
          "from": "logistics.inventory-ledger",
          "to": "civic.role-protocol",
          "kind": "supports",
          "reason": "W4 的库存谱系证明谁取走、加工和归还公共物资，为角色权限和失约结算提供依据。"
        },
        {
          "from": "quantum.quantum-storage",
          "to": "chronology.chrono-clock",
          "kind": "supports",
          "reason": "量子高密度状态只有在时钟安排的窗口内交接，周期为跨距传输提供背压和去重点。"
        },
        {
          "from": "chronology.history-archive",
          "to": "civic.role-protocol",
          "kind": "supports",
          "reason": "历史档案为角色争议、权限撤销和故障归责提供共同事实，避免多人协作退化为不可审计的口头约定。"
        },
        {
          "from": "civic.energy-charter",
          "to": "quantum.decoherence",
          "kind": "supports",
          "reason": "公共优先级决定能源不足时先保护哪些量子状态；能源紧张会让退相干层安全冻结，而不是强行写入。"
        },
        {
          "from": "civic.role-protocol",
          "to": "quantum.quantum-storage",
          "kind": "supports",
          "reason": "任何量子状态的读写和公共周期提交都先验证最小权限，角色撤销即可阻止失效端点继续消费库存。"
        },
        {
          "from": "metallurgy.smelting-control",
          "to": "chronology.history-archive",
          "reason": "W5 的热史与性质证书进入历史档案，终局设备只能消费来源和材料性质均可复核的批次。",
          "kind": "supports"
        },
        {
          "from": "mechanics.maintenance",
          "to": "chronology.history-archive",
          "reason": "维护窗口的前后状态与拆件回收记录进入历史档案，证明 W5 的能力仍有可交接的维护责任。",
          "kind": "supports"
        },
        {
          "from": "exploration.ruin-scan",
          "to": "chronology.history-archive",
          "reason": "W6 返程扫描的样本清单和领取状态写入历史档案，重复访问只能复用既有记录。",
          "kind": "supports"
        },
        {
          "from": "space.rift-safety",
          "to": "civic.role-protocol",
          "reason": "W7 已知端点的安全证明和 recoveryKey 限定运输责任范围，角色撤销即可阻止失效端点继续消费库存。",
          "kind": "supports"
        },
        {
          "from": "astral.star-map",
          "to": "chronology.history-archive",
          "reason": "W8 星图的最后有效读数和校准窗口成为下一周期可复用的导航证据。",
          "kind": "supports"
        },
        {
          "from": "dimensional.return-protocol",
          "to": "civic.role-protocol",
          "reason": "W8 远征结案把实际到达、货单与异常交给公共角色协议，未结案载荷不能转入公共服务。",
          "kind": "supports"
        }
      ],
      "anchors": [
        {
          "disciplineId": "quantum",
          "familyId": "quantum-storage",
          "familyKey": "quantum.quantum-storage",
          "reason": "把量子工程的核心限定为有限、可归属、可解包的高密度状态，直接回应旧文明把高密度技术误当成无限产能的失败。",
          "stories": [
            {
              "itemId": "quantum.quantum-storage.bit",
              "order": 1,
              "text": "让复原者把一批已有来源的物资状态写入有限槽位；动机是减少跨区交接，而代价是先分类并登记，量子层不能凭空产生资源。"
            },
            {
              "itemId": "quantum.quantum-storage.core",
              "order": 2,
              "text": "把多个已登记作业的状态合并为可校验快照，消耗 `technology.energy-storage` 的预留和 `materials.reinforced-thread` 的容纳能力；源端在提交中途变化时保留旧版本并退回未提交输入。"
            },
            {
              "itemId": "quantum.quantum-storage.gate",
              "order": 3,
              "text": "才能把密集快照交给公共服务；两端授权、版本和归属都必须匹配，成功读取后可拆回原物资，目标拒绝时回滚到源端，证明压缩不是资源黑洞。"
            }
          ]
        },
        {
          "disciplineId": "quantum",
          "familyId": "decoherence",
          "familyKey": "quantum.decoherence",
          "reason": "把退相干从装饰性的宇宙风险变成终局的故障边界和恢复演练，保证量子层持续消耗前八波成果却不吞掉它们。",
          "stories": [
            {
              "itemId": "quantum.decoherence.bit",
              "order": 1,
              "text": "在写入前检查稳定性和授权；条件不满足时冻结并说明原因，教玩家把停机当作保护而不是继续抽取。"
            },
            {
              "itemId": "quantum.decoherence.core",
              "order": 2,
              "text": "将不稳定分支与正常链隔离，并把损失的吞吐作为明确代价；它回指 `energy.grid-protection`，让故障只伤及局部而不拖垮城邦。"
            },
            {
              "itemId": "quantum.decoherence.gate",
              "order": 3,
              "text": "用一次受控单节点故障演练冻结、告警、恢复和单次重放；修复后快照版本、物资归属和能量账目一致，若出现重复扣料则说明旧教训仍未被吸收。"
            }
          ]
        },
        {
          "disciplineId": "chronology",
          "familyId": "chrono-clock",
          "familyKey": "chronology.chrono-clock",
          "reason": "把时间学落到可重复的服务周期与不重叠任务上，避免用加速世界时间掩盖资源消耗、维护和恢复。",
          "stories": [
            {
              "itemId": "chronology.chrono-clock.part",
              "order": 1,
              "text": "为一项食物、能源或维护服务定义可观察的周期，不改世界时间；动机是让玩家理解等待和保留容量本身就是成本。"
            },
            {
              "itemId": "chronology.chrono-clock.drive",
              "order": 2,
              "text": "串行提交跨学科任务，输入只消费一次，未完成任务延期而非重启；`agroecology.compost-loop` 与 `hydrology.reservoir` 的回流必须在周期结算时登记。"
            },
            {
              "itemId": "chronology.chrono-clock.workstation",
              "order": 3,
              "text": "编排城邦的生产、补给、维护和公共服务阶段；周期中断时从检查点继续，周期结束把剩余库存和副产物交回下一周期，而不是生成一次性终局奖励。"
            }
          ]
        },
        {
          "disciplineId": "chronology",
          "familyId": "history-archive",
          "familyKey": "chronology.history-archive",
          "reason": "让“旧文明为何崩解”成为可查询、可比较、能改变下一周期的运行记忆，避免历史只承担背景叙事。",
          "stories": [
            {
              "itemId": "chronology.history-archive.probe",
              "order": 1,
              "text": "记录一项输入的来源、操作者、初始状态和用途；动机是让玩家能解释每份材料为何进入终局，代价是必须留下可查询的历史。"
            },
            {
              "itemId": "chronology.history-archive.analyzer",
              "order": 2,
              "text": "对照成功周期与失败周期，找出过载、错配和重复提交；它把 `automation.recovery` 与 `logistics.inventory-ledger` 的事件转成可执行的改进规则，而非一段纯传说。"
            },
            {
              "itemId": "chronology.history-archive.station",
              "order": 3,
              "text": "保存城邦可复用的最后有效快照、角色和能源状态；故障后只重放未应用步骤，分歧输入退回原所有者，历史因此成为恢复操作的依据。"
            }
          ]
        },
        {
          "disciplineId": "civic",
          "familyId": "role-protocol",
          "familyKey": "civic.role-protocol",
          "reason": "把多人长期协作从口头约定变成最小权限、可撤销、能处理失约的制度，防止公共设施再次被无界自动化或个人权限劫持。",
          "stories": [
            {
              "itemId": "civic.role-protocol.token",
              "order": 1,
              "text": "为一次明确的公共操作授予最小范围权限，区分个人、工坊和公共库存；动机是让多人有事可做，代价是先同意并登记，而不是共享万能钥匙。"
            },
            {
              "itemId": "civic.role-protocol.contract",
              "order": 2,
              "text": "把协作写成输入、输出、期限和失败回收条件；交接失败时输入回到 `civic.public-inventory`，责任留在账上而不是凭空消失。"
            },
            {
              "itemId": "civic.role-protocol.exchange",
              "order": 3,
              "text": "将量子交接、时间周期和公共服务放入可撤销且留痕的授权流程；发生故障时可以撤销失效权限、结算未完成义务并让其他角色继续运营。"
            }
          ]
        },
        {
          "disciplineId": "civic",
          "familyId": "energy-charter",
          "familyKey": "civic.energy-charter",
          "reason": "使城邦工程承担终局的守恒分配：持续消耗前八波的能源、材料和副产物，同时保留关键服务与可恢复储备。",
          "stories": [
            {
              "itemId": "civic.energy-charter.coil",
              "order": 1,
              "text": "把照明、救援、周期调度和量子稳定列为不同负载，并沿用 `technology.energy-storage` 与 `energy.grid-protection` 的边界；动机是先决定保什么，代价是显式预留而非免费供能。"
            },
            {
              "itemId": "civic.energy-charter.cell",
              "order": 2,
              "text": "在周期之间保存公共储备，按优先级延后非关键任务，并从 `energy.biomass`、`hydrology.reservoir` 等早期循环补回消耗；储备占用本身是维持韧性的机会成本。"
            },
            {
              "itemId": "civic.energy-charter.unit",
              "order": 3,
              "text": "执行一次完整服务周期中的公共供能章程；单节点故障时隔离非关键支路、保住照明与救援，修复后把可回收能量和副产物重新计入下一周期，证明城邦不是靠永久透支运行。"
            }
          ]
        }
      ]
    }
  ]
};

function deepFreeze(value) {
  if (!value || typeof value !== 'object' || Object.isFrozen(value)) return value;
  Object.freeze(value);
  for (const child of Object.values(value)) deepFreeze(child);
  return value;
}

const CAMPAIGN = deepFreeze(RAW_CAMPAIGN.campaign);
const CAMPAIGN_ACTS = deepFreeze(RAW_CAMPAIGN.acts);
const CAMPAIGN_WAVES = deepFreeze(RAW_CAMPAIGN.waves);

export { CAMPAIGN, CAMPAIGN_ACTS, CAMPAIGN_WAVES };
