<template>
  <a-card :bordered="false">

    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="8" :sm="24">
            <a-form-item label="用户名">
              <a-input v-model="queryParam.username" placeholder="请输入用户名" allowClear/>
            </a-form-item>
          </a-col>
          <a-col :md="!advanced && 8 || 24" :sm="24">
            <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
              <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => queryParam = {}">重置</a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="handleNew()">新建</a-button>
      <!--      <a-dropdown v-action:edit v-if="selectedRowKeys.length > 0">-->
      <!--        <a-menu slot="overlay">-->
      <!--          <a-menu-item key="1"><a-icon type="delete" />删除</a-menu-item>-->
      <!--          &lt;!&ndash; lock | unlock &ndash;&gt;-->
      <!--          <a-menu-item key="2"><a-icon type="lock" />锁定</a-menu-item>-->
      <!--        </a-menu>-->
      <!--        <a-button style="margin-left: 8px">-->
      <!--          批量操作 <a-icon type="down" />-->
      <!--        </a-button>-->
      <!--      </a-dropdown>-->
    </div>

    <s-table
      ref="table"
      size="default"
      rowKey="key"
      :columns="columns"
      :data="loadData"
      :alert="options.alert"
      :rowSelection="options.rowSelection"
    >
      <span slot="serial" slot-scope="text, record">
        {{ record.id }}
      </span>
      <span slot="groupNameList" slot-scope="text, record">
        {{ record.role === 2 ? '所有组' : text.toString() }}
      </span>
      <span slot="role" slot-scope="text, record">
        {{ record.role === 2 ? '管理员' : '普通用户' }}
      </span>
      <span slot="action" slot-scope="text, record">
        <template>
          <a @click="handleEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm
            title="是否确认删除这个用户吗?"
            ok-text="删除"
            cancel-text="取消"
            @confirm="handleDel(record)">
            <a href="javascript:;">删除</a>
          </a-popconfirm>

        </template>
      </span>
    </s-table>
  </a-card>
</template>

<script>

import ATextarea from 'ant-design-vue/es/input/TextArea'
import AInput from 'ant-design-vue/es/input/Input'
import moment from 'moment'
// 动态切换组件
import { getUserPage, delUser } from '@/api/manage'
import { STable } from '@/components'

export default {
  name: 'TableListWrapper',
  components: {
    AInput,
    ATextarea,
    STable
  },
  data () {
    return {
      currentComponet: 'List',
      record: '',
      mdl: {},
      // 高级搜索 展开/关闭
      advanced: false,
      // 查询参数
      queryParam: {},
      // 表头
      columns: [
        {
          title: '#',
          width: '5%',
          scopedSlots: { customRender: 'serial' }
        },
        {
          title: '用户名',
          width: '12%',
          dataIndex: 'username'
        },
        {
          title: '角色',
          dataIndex: 'role',
          width: '10%',
          scopedSlots: { customRender: 'role' }
        },
        {
          title: '权限',
          dataIndex: 'groupNameList',
          width: '45%',
          scopedSlots: { customRender: 'groupNameList' }
        },
        {
          title: '更新时间',
          width: '18%',
          dataIndex: 'updateDt',
          customRender: (text) => moment(text).format('YYYY-MM-DD HH:mm:ss')
        },
        {
          title: '操作',
          width: '10%',
          dataIndex: 'action',
          scopedSlots: { customRender: 'action' }
        }
      ],
      // 加载数据方法 必须为 Promise 对象
      loadData: parameter => {
        console.log('loadData.parameter', parameter)
        return getUserPage(Object.assign(parameter, this.queryParam))
          .then(res => {
            return res
          })
      },
      selectedRowKeys: [],
      selectedRows: [],

      // custom table alert & rowSelection
      options: {
        alert: { show: true, clear: () => { this.selectedRowKeys = [] } },
        rowSelection: {
          selectedRowKeys: this.selectedRowKeys,
          onChange: this.onSelectChange
        }
      },
      optionAlertShow: false
    }
  },
  filters: {
    // filterTime(time) {
    //   return moment(time).format("YYYY-MM-DD HH:mm:ss")
    // }
  },
  methods: {
    handleNew () {
      this.$router.push('/user-form')
    },
    handleEdit (record) {
      this.record = record || ''
      this.$router.push({ path: '/user-form', query: { username: record.username } })
    },
    handleDel (record) {
      delUser(record.id).then(res => {
        this.$refs.table.refresh(true)
        this.$message.success(`删除成功`)
      })
    },
    handleGoBack () {
      this.record = ''
      this.currentComponet = 'List'
    }
  },
  watch: {
    '$route.path' () {
      this.record = ''
      this.currentComponet = 'List'
    }
  }
}
</script>
