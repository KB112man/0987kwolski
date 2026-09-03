sed -i 's/private val _showLayoffDestinationDialog = MutableStateFlow<Card?>(null)/private val _isLayoffDialogOpen = MutableStateFlow(false)/g' app/src/main/java/com/example/ui/GameViewModel.kt
sed -i 's/val showLayoffDestinationDialog: StateFlow<Card?> = _showLayoffDestinationDialog.asStateFlow()/val isLayoffDialogOpen: StateFlow<Boolean> = _isLayoffDialogOpen.asStateFlow()/g' app/src/main/java/com/example/ui/GameViewModel.kt
sed -i 's/_showLayoffDestinationDialog.value = card/_isLayoffDialogOpen.value = true/g' app/src/main/java/com/example/ui/GameViewModel.kt
sed -i 's/_showLayoffDestinationDialog.value = null/_isLayoffDialogOpen.value = false/g' app/src/main/java/com/example/ui/GameViewModel.kt
