<script setup lang="ts">
// 空白布局不渲染菜单和头部，只提供登录等独立页面的全高路由容器。
defineOptions({ name: 'BlankLayout' })
</script>

<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<style scoped>
.blank-layout {
  height: 100%;
}
</style>
