<script setup lang="ts">
defineOptions({ name: 'TablePager' })
defineProps<{ state: any }>()
</script>

<template>
  <el-pagination
    class="table-pager"
    background
    layout="total, sizes, prev, pager, next"
    :total="state.pagination.value.total"
    :current-page="state.pagination.value.pageNo"
    :page-size="state.pagination.value.pageSize"
    @current-change="(pageNo: number) => state.onPageChange({ pageNo, pageSize: state.pagination.value.pageSize })"
    @size-change="(pageSize: number) => state.onPageChange({ pageNo: 1, pageSize })"
  />
</template>

<style scoped>
.table-pager { margin-top: 16px; justify-content: flex-end; }
</style>
